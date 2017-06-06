/**
 * The BSD License
 *
 * Copyright (c) 2010-2012 RIPE NCC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   - Neither the name of the RIPE NCC nor the names of its contributors may be
 *     used to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.ripe.rpki.validator
package config

import java.io.{File, PrintStream}
import java.util.EnumSet
import javax.servlet.DispatcherType

import com.jcabi.ssh.{SSHByPassword, Shell}
import grizzled.slf4j.Logging
import net.ripe.ipresource.Asn
import net.ripe.rpki.validator.RoaBgpIssues.RoaBgpIssueSeeker
import net.ripe.rpki.validator.api.RestApi
import net.ripe.rpki.validator.bgp.preview._
import net.ripe.rpki.validator.config.health.HealthServlet
import net.ripe.rpki.validator.fetchers.FetcherConfig
import net.ripe.rpki.validator.iana.block.{IanaAnnouncementSet, IanaAnnouncementValidator, IanaDumpDownloader}
import net.ripe.rpki.validator.lib.RoaOperationMode.RoaOperationMode
import net.ripe.rpki.validator.lib.{UserPreferences, _}
import net.ripe.rpki.validator.models.validation._
import net.ripe.rpki.validator.models.{Idle, IgnoreFilter, TrustAnchorData, _}
import net.ripe.rpki.validator.ranking.{RankingDumpDownloader, RankingSet}
import net.ripe.rpki.validator.rtr.{Pdu, RTRServer}
import net.ripe.rpki.validator.store.{CacheStore, DurableCaches}
import net.ripe.rpki.validator.util.TrustAnchorLocator
import org.apache.commons.io.FileUtils
import org.eclipse.jetty.server.Server
import org.joda.time.{DateTime, Period}
import org.slf4j.LoggerFactory

import scala.Predef._
import scala.collection.JavaConverters._
import scala.collection.SortedMap
import scala.concurrent.Future
import scala.concurrent.stm._
import scala.math.Ordering.Implicits._
import scalaz.{Failure, Success}

object Main {
  private val sessionId: Pdu.SessionId = Pdu.randomSessionid

  def main(args: Array[String]): Unit = {
    setupLogging()
    new Main()
  }

  private def setupLogging() {
    System.setProperty("VALIDATOR_LOG_FILE", ApplicationOptions.applicationLogFileName)
    System.setProperty("RTR_LOG_FILE", ApplicationOptions.rtrLogFileName)
    System.setErr(new PrintStream(new LoggingOutputStream(), true))
    LoggerFactory.getLogger(this.getClass).info("Starting up the RPKI validator...")
  }
}

class Main extends Http with Logging { main =>
  import scala.concurrent.duration._

  implicit val actorSystem = akka.actor.ActorSystem()
  import actorSystem.dispatcher
  val startedAt = System.currentTimeMillis

  logger.info(s"Prefer RRDP=${ApplicationOptions.preferRrdp}")
  
  val bgpAnnouncementSets = Ref(Seq(
    BgpAnnouncementSet("http://www.ris.ripe.net/dumps/riswhoisdump.IPv4.gz"),
    BgpAnnouncementSet("http://www.ris.ripe.net/dumps/riswhoisdump.IPv6.gz")))

  val ianaSets = Ref(Seq(
    IanaAnnouncementSet("http://www.iana.org/assignments/ipv4-address-space/ipv4-address-space.xml"),
    IanaAnnouncementSet("http://www.iana.org/assignments/ipv6-address-space/ipv6-address-space.xml")))

  val rankingSets = Ref(Seq(
    RankingSet("http://bgpranking.circl.lu/json")))

  val bgpAnnouncementValidator = new BgpAnnouncementValidator
  val ianaAnnouncementValidator = new IanaAnnouncementValidator
  val roaBgpIssueSeeker = new RoaBgpIssueSeeker


  val dataFile = ApplicationOptions.dataFileLocation
  val data = PersistentDataSerialiser.read(dataFile).getOrElse(PersistentData())

  val trustAnchors = loadTrustAnchors().all.map { ta => ta.copy(enabled = data.trustAnchorData.get(ta.name).forall(_.enabled)) }

  val roas = ValidatedObjects(new TrustAnchors(trustAnchors.filter(_.enabled)))

  override def trustedCertsLocation = ApplicationOptions.trustedSslCertsLocation

  val userPreferences = Ref(data.userPreferences)

  val bgpRisDumpDownloader = new BgpRisDumpDownloader(http)

  val ianaDumpDownloader = new IanaDumpDownloader()

  val rankingDumpDownloader = new RankingDumpDownloader(http)

  var timlineConflictsRecords = (List("No Data"),List(List(0)))

  protected var lastState: RoaOperationMode = null;


  val memoryImage = Ref(
    MemoryImage(data.filters, data.whitelist, new TrustAnchors(trustAnchors), roas, data.blockList, data.asRankings,data.blockAsList,data.suggestedRoaFilterList,data.pathEndTable,data.localPathEndNeighbors, data.suggestedWhitelistASN))

  var store : CacheStore = _

  def updateMemoryImage(f: MemoryImage => MemoryImage)(implicit transaction: MaybeTxn) {
    atomic { implicit transaction =>
      val oldVersion = memoryImage().version
      memoryImage.transform(f)
      val newVersion = memoryImage().version

      val bgpAnnouncements = main.bgpAnnouncementSets().flatMap(_.entries)
      val distinctRtrPrefixes = memoryImage().getDistinctRtrPrefixes

      Txn.afterCommit { _ =>
        if (oldVersion != newVersion) {
          if(bgpAnnouncements.isEmpty){
            refreshRisDumps() //TODO: make some kind of failure rule to disengage in-case it's stuck and repetitive
          }
          bgpAnnouncementValidator.startUpdate(bgpAnnouncements, distinctRtrPrefixes.toSeq)
          bgpAnnouncementValidator.updateRoaBgpConflictsSet(userPreferences.single.get.maxConflictedBgpStaleDays)
          rtrServer.notify(newVersion)
        }
      }
    }
  }


  wipeRsyncDiskCache()

  val rtrServer = runRtrServer()
  runWebServer()
//  actorSystem.scheduler.schedule(initialDelay = 0.seconds, interval = 60  .seconds) { connectToRouter() }
  actorSystem.scheduler.schedule(initialDelay = 300.seconds, interval = 2.minutes) { updateFilters(true) }
  actorSystem.scheduler.schedule(initialDelay = 100.seconds, interval = 2.minutes) { updateSuggestedWhitelistRecords() }
  actorSystem.scheduler.schedule(initialDelay = 200.seconds, interval = 3.minutes) { updateTimeLine() }
  actorSystem.scheduler.schedule(initialDelay = 120.seconds, interval = 0.5.hours) { refreshRankingDumps() }
  actorSystem.scheduler.schedule(initialDelay = 0.seconds, interval = 4.hours) { refreshIanaDumps() }
  actorSystem.scheduler.schedule(initialDelay = 0.seconds, interval = 10.seconds) { runValidator(false) }
  actorSystem.scheduler.schedule(initialDelay = 0.seconds, interval = 2.hours) { refreshRisDumps() }

  private def updateTimeLine(): Unit ={
    bgpAnnouncementValidator.roaBgpIssuesSet
    var timeLineList = List[Long]()
    bgpAnnouncementValidator.roaBgpIssuesSet.roaBgpIssuesSet.toArray.foreach(x=> x.bgpAnnouncements.foreach(y=> timeLineList ++= List(((y._3.toDate.getTime)/10000)*10000)))
    var timeLineMap = timeLineList.groupBy(identity).mapValues(_.size)
    var sorted = SortedMap[Long, Int]() ++ timeLineMap
    var values =  sorted.valuesIterator.toList
    var labels_long = sorted.keysIterator.toList
    var labels = List[String]()
    labels_long.foreach(x=> labels ++= List((new DateTime(x)).toString("d/MM/yy - kk:mm")))
    timlineConflictsRecords = (labels.takeRight(7),List(values.takeRight(7)))
  }
  private def connectToRouter(): Unit = {
    try {
      val shell = new SSHByPassword("bgpsafe", 22, "fima", "1987");
      val stdout = new Shell.Plain(shell).exec("show ip interface brief");
      logger.info(stdout);
    }catch {
      case e: Exception => println("Error: " + e);
    }
  }

  private def isBgpIssueOld(recordValidationDate: DateTime):Boolean = {
    val currentTime = DateTime.now()
    val timeDifference = new Period(currentTime, recordValidationDate)
    if(timeDifference.getDays() >= userPreferences.single.get.conflictCertDays ){
      return false
    }
    true
  }

  private def updateSuggestedWhitelistRecords() {
    //TODO try to move to += now the set is mutable
      //See if the user wants automatic whitelisting of long term conflicting ROA's
      if(userPreferences.single.get.roaBgpConflictLearnMode){
        val safeDays = userPreferences.single.get.conflictCertDays
        bgpAnnouncementValidator.roaBgpIssuesSet.roaBgpIssuesSet.foreach(x => x.bgpAnnouncements --= x.bgpAnnouncements.filter(y => isBgpIssueOld(y._3)))
        val filteredIssueSet = bgpAnnouncementValidator.roaBgpIssuesSet.roaBgpIssuesSet.filterNot(x => x.bgpAnnouncements.isEmpty)
        memoryImage.single.get.suggestedWhitelistASN.clearEntries
        filteredIssueSet.foreach(x => x.bgpAnnouncements.foreach(y => {
          //memoryImage.single.get.addSuggestedWhitelistEntry(new RtrPrefix(y._2.asn, y._2.prefix))
          updateMemoryImage(_.addSuggestedWhitelistEntry(new RtrPrefix(y._2.asn, y._2.prefix)))
          //addSuggestedWhitelistEntry
        }))
//        updateMemoryImage(_.updateSuggestedWhitelistASN(suggestedWhitelistASN))
      }
  }

  private def loadTrustAnchors(): TrustAnchors = {
    val tals = FileUtils.listFiles(ApplicationOptions.talDirLocation, Array("tal"), false)
    TrustAnchors.load(tals.asScala.toSeq)
  }

  private def refreshRisDumps() {
    Future.traverse(bgpAnnouncementSets.single.get)(bgpRisDumpDownloader.download) foreach { dumps =>
      atomic { implicit transaction =>
        bgpAnnouncementSets() = dumps
        bgpAnnouncementValidator.startUpdate(bgpAnnouncementSets().flatMap(_.entries), memoryImage().getDistinctRtrPrefixes.toSeq)
        bgpAnnouncementValidator.updateRoaBgpConflictsSet(userPreferences.single.get.maxConflictedBgpStaleDays)

      }
    }
  }

  private def refreshRankingDumps() {
    Future.traverse(rankingSets.single.get)(rankingDumpDownloader.download) foreach { dumps =>
      atomic { implicit transaction =>
        rankingSets() = dumps

      }
    }
  }

  private def refreshIanaDumps() {
    Future.traverse(ianaSets.single.get) (ianaDumpDownloader.download) foreach { dumps =>
      atomic { implicit transaction =>
            ianaSets() = dumps
//            ianaAnnouncementValidator.startUpdate(ianaSets().flatMap(_.entries), memoryImage().getDistinctRtrPrefixes.toSeq)
      }
    }
  }
   private def updateFilters(forceUpdate: Boolean = false) = {
    if(lastState == null || main.userPreferences.single.get.roaOperationMode != lastState || forceUpdate){
      if(main.userPreferences.single.get.roaOperationMode == RoaOperationMode.ManualMode){
        memoryImage.single.get.suggestedRoaFilterList.entries =  scala.collection.mutable.Set.empty
        memoryImage.single.get.filters.entries = scala.collection.mutable.Set.empty
        var defaultMaxLen: Int = 0
        for(entry <- bgpAnnouncementValidator.roaBgpIssuesSet.roaBgpIssuesSet){
          memoryImage.single.get.suggestedRoaFilterList.entries += new SuggestedRoaFilter(entry.roa.asn,entry.roa.prefix,entry.roa.maxPrefixLength.getOrElse[Int](0))
          lastState =  RoaOperationMode.ManualMode
        }
      }
      if(main.userPreferences.single.get.roaOperationMode == RoaOperationMode.AutoModeRemoveBadROA){
        for(entry <- memoryImage.single.get.suggestedRoaFilterList.entries) {
          if(!memoryImage.single.get.filters.entries.contains(new IgnoreFilter(entry.prefix))){
            entry.block = true
            memoryImage.single.get.filters.entries += new IgnoreFilter(entry.prefix)
          }
        }
        lastState =  RoaOperationMode.AutoModeRemoveBadROA
      }
    }
  }


  private def runValidator(forceNewFetch: Boolean) {
    import lib.DateAndTime._

    val now = new DateTime
    val needUpdating = for {
      ta <- memoryImage.single.get.trustAnchors.all if ta.status.isIdle
      Idle(nextUpdate, _) = ta.status
      if nextUpdate <= now
    } yield ta.name

    runValidator(needUpdating, forceNewFetch)
  }

  private def runValidator(trustAnchorNames: Seq[String], forceNewFetch: Boolean) {
    val maxStaleDays = userPreferences.single.get.maxStaleDays
    val trustAnchors = memoryImage.single.get.trustAnchors.all

    val taLocators = trustAnchorNames.flatMap { name => trustAnchors.find(_.name == name) }

    store = DurableCaches(ApplicationOptions.workDirLocation)

    for (trustAnchorLocator <- taLocators) {
      Future {
        val repoService = new RepoService(RepoFetcher(ApplicationOptions.workDirLocation, FetcherConfig(ApplicationOptions.rsyncDirLocation)))

        val process = new TrustAnchorValidationProcess(trustAnchorLocator.locator,
          store,
          repoService,
          maxStaleDays,
          trustAnchorLocator.name,
          ApplicationOptions.enableLooseValidation
        ) with TrackValidationProcess with ValidationProcessLogger {
          override val memoryImage = main.memoryImage
        }
        try {
          process.runProcess(forceNewFetch) match {
            case Success(validatedObjects) =>
              updateMemoryImage(_.updateValidatedObjects(trustAnchorLocator.locator, validatedObjects))
            case Failure(_) =>
          }
        } finally {
          process.shutdown()
        }
      }
    }
  }

  private def runWebServer() {
    val server = setup(new Server(ApplicationOptions.httpPort))

    sys.addShutdownHook({
      server.stop()
      logger.info("Terminating...")
    })
    server.start()
    logger.info("Welcome to the RIPE NCC RPKI Validator, now available on port " + ApplicationOptions.httpPort + ". Hit CTRL+C to terminate.")
  }


  private def runRtrServer(): RTRServer = {
    val rtrServer = new RTRServer(
      port = ApplicationOptions.rtrPort,
      closeOnError = ApplicationOptions.rtrCloseOnError,
      sendNotify = ApplicationOptions.rtrSendNotify,
      getCurrentCacheSerial = {
        () => memoryImage.single.get.version
      },
      getCurrentRtrPrefixes = {
        () => memoryImage.single.get.getDistinctRtrPrefixes
      },
      getCurrentSessionId = {
        () => Main.sessionId
      },
      hasTrustAnchorsEnabled = {
        () => memoryImage.single.get.trustAnchors.hasEnabledAnchors
      })
    rtrServer.startServer()
    rtrServer
  }

  private def setup(server: Server): Server = {
    import org.eclipse.jetty.server.NCSARequestLog
    import org.eclipse.jetty.server.handler.{HandlerCollection, RequestLogHandler}
    import org.eclipse.jetty.servlet._
    import org.scalatra._

    val webFilter = new WebFilter {
      private val dataFileLock = new Object()
      private def updateAndPersist(f: InTxn => Unit) {
        dataFileLock synchronized {
          val (image, userPreferences) = atomic { implicit transaction =>
            f(transaction)
            (memoryImage.get, main.userPreferences.get)
          }
          PersistentDataSerialiser.write(
            PersistentData(filters = image.filters, whitelist = image.whitelist, userPreferences = userPreferences,
              trustAnchorData = image.trustAnchors.all.map(ta => ta.name -> TrustAnchorData(ta.enabled))(collection.breakOut)), dataFile)
        }
      }
      override protected def startTrustAnchorValidation(trustAnchors: Seq[String]) = main.runValidator(trustAnchors, true)

      override protected def trustAnchors = memoryImage.single.get.trustAnchors
      override protected def validatedObjects = memoryImage.single.get.validatedObjects

      override protected def filters = memoryImage.single.get.filters
      override protected def addSuggestedRoaFilter(filter: SuggestedRoaFilter) = updateAndPersist { implicit transaction => updateMemoryImage(_.addSuggestedRoaFilter(filter)) }
      override protected def removeSuggestedRoaFilter(filter: SuggestedRoaFilter) = updateAndPersist { implicit transaction => updateMemoryImage(_.removeSuggestedRoaFilter(filter)) }
      override protected def addPathEndRecord(filter: PathEndRecord) = updateAndPersist { implicit transaction => updateMemoryImage(_.addPathEndRecord(filter)) }
      override protected def removePathEndRecord(filter: PathEndRecord) = updateAndPersist { implicit transaction => updateMemoryImage(_.removePathEndRecord(filter)) }
      override protected def addPathEndNeighbor(entry: Asn) = updateAndPersist { implicit transaction => updateMemoryImage(_.addPathEndNeighbor(entry)) }
      override protected def removePathEndNeighbor(entry: Asn) = updateAndPersist { implicit transaction => updateMemoryImage(_.removePathEndNeighbor(entry)) }
      override protected def addFilter(filter: IgnoreFilter) = updateAndPersist { implicit transaction => updateMemoryImage(_.addFilter(filter)) }
      override protected def removeFilter(filter: IgnoreFilter) = updateAndPersist { implicit transaction => updateMemoryImage(_.removeFilter(filter)) }

      override protected def whitelist = memoryImage.single.get.whitelist
      override protected def addWhitelistEntry(entry: RtrPrefix) = updateAndPersist { implicit transaction => updateMemoryImage(_.addWhitelistEntry(entry)) }
      override protected def removeWhitelistEntry(entry: RtrPrefix) = updateAndPersist { implicit transaction => updateMemoryImage(_.removeWhitelistEntry(entry)) }


      override protected def blockList = memoryImage.single.get.blockList
      override protected def blockAsList = memoryImage.single.get.blockAsList

      override protected def addBlockListEntry(entry: BlockFilter): Unit = updateAndPersist { implicit transaction => updateMemoryImage(_.addBlocklistEntry(entry)) }
      override protected def removeBlockListEntry(entry: BlockFilter): Unit = updateAndPersist { implicit transaction => updateMemoryImage(_.removeBlocklistEntry(entry)) }

      override protected def addBlockAsListEntry(entry: BlockAsFilter): Unit = updateAndPersist { implicit transaction => updateMemoryImage(_.addBlockAslistEntry(entry)) }
      override protected def removeBlockAsListEntry(entry: BlockAsFilter): Unit = updateAndPersist { implicit transaction => updateMemoryImage(_.removeBlockAslistEntry(entry)) }

      override protected def bgpAnnouncementSet = main.bgpAnnouncementSets.single.get
      override protected def timelineConflicts = main.timlineConflictsRecords
      override protected def validatedAnnouncements = bgpAnnouncementValidator.validatedAnnouncements
      override protected def roaBgpIssuesSet = bgpAnnouncementValidator.roaBgpIssuesSet

      override protected def validatedIanaSets = ianaSets.single.get

      override protected def aSrankingSets = rankingSets.single.get

      override protected def getRtrPrefixes = memoryImage.single.get.getDistinctRtrPrefixes

      override protected def suggestedWhitelistASN = memoryImage.single.get.suggestedWhitelistASN

      protected def sessionData = rtrServer.rtrSessions.allClientData



      // Software Update checker
      override def newVersionDetailFetcher = new OnlineNewVersionDetailFetcher(ReleaseInfo.version,
        () => {
          val response = httpGet("https://lirportal.ripe.net/certification/content/static/validator/latest-version.properties")
          scala.io.Source.fromInputStream(response.getEntity.getContent).mkString
        })

      // UserPreferences
      override def userPreferences = main.userPreferences.single.get
      override def updateUserPreferences(userPreferences: UserPreferences) = {
        if(!userPreferences.roaBgpConflictLearnMode){
          memoryImage.single.get.suggestedWhitelistASN.copy(scala.collection.mutable.Set.empty[RtrPrefix])
        }
        updateAndPersist { implicit transaction => main.userPreferences.set(userPreferences) }
      }



      override protected def updateFilters(forceUpdate: Boolean = false) = main.updateFilters(forceUpdate)

      override protected def updateTrustAnchorState(locator: TrustAnchorLocator, enabled: Boolean) = updateAndPersist { implicit transaction =>
        memoryImage.transform(_.updateTrustAnchorState(locator, enabled))
      }

      override protected def asRankings: AsRankings =  memoryImage.single.get.asRankings
      //TODO bring back roa blacklist.
      override protected def suggestedRoaFilters: SuggestedRoaFilterList =  memoryImage.single.get.suggestedRoaFilterList
      override protected def pathEndTable: PathEndTable =  memoryImage.single.get.pathEndTable
      override protected def localPathEndNeighbors: LocalPathEndNeighbors =  memoryImage.single.get.localPathEndNeighbors

    }

    val restApiServlet = new RestApi() {
      override protected def getVrpObjects = memoryImage.single.get.getDistinctRtrPrefixes

      override protected def getCachedObjects = store.getAllObjects
    }



    val root = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS)
    root.setResourceBase(getClass.getResource("/public").toString)
    val defaultServletHolder = new ServletHolder(new DefaultServlet())
    defaultServletHolder.setName("default")
    defaultServletHolder.setInitParameter("dirAllowed", "false")
    root.addServlet(defaultServletHolder, "/*")
    root.addServlet(new ServletHolder(restApiServlet), "/api/*")
    root.addServlet(new ServletHolder(new HealthServlet()), "/health")
    root.addFilter(new FilterHolder(webFilter), "/*", EnumSet.allOf(classOf[DispatcherType]))

    val requestLogHandler = {
      val handler = new RequestLogHandler()
      val requestLog = new NCSARequestLog(ApplicationOptions.accessLogFileName)
      requestLog.setAppend(true)
      requestLog.setExtended(true)
      requestLog.setLogLatency(true)
      requestLog.setPreferProxiedForAddress(true)
      requestLog.setRetainDays(90)
      handler.setRequestLog(requestLog)
      handler
    }

    val handlers = new HandlerCollection()
    handlers.addHandler(root)
    handlers.addHandler(requestLogHandler)
    server.setHandler(handlers)
    server
  }

  private def wipeRsyncDiskCache() {
    val diskCache = new File(ApplicationOptions.rsyncDirLocation)
    if (diskCache.isDirectory) {
      FileUtils.cleanDirectory(diskCache)
    }
  }





}


//
//override def updateFilters() = {
//  if(lastState == null || userPreferences.single.get.roaOperationMode != lastState){
//  if(userPreferences.single.get.roaOperationMode == RoaOperationMode.ManualMode){
//  memoryImage.single.get.suggestedRoaFilterList.entries =  scala.collection.mutable.Set.empty
//  memoryImage.single.get.filters.entries = scala.collection.mutable.Set.empty
//  var defaultMaxLen: Int = 0
//  for(entry <- bgpAnnouncementValidator.roaBgpIssuesSet.roaBgpIssuesSet){
//  memoryImage.single.get.suggestedRoaFilterList.entries += new SuggestedRoaFilter(entry.roa.asn,entry.roa.prefix,entry.roa.maxPrefixLength.getOrElse[Int](0))
//  lastState =  RoaOperationMode.ManualMode
//}
//}
//  if(userPreferences.single.get.roaOperationMode == RoaOperationMode.AutoModeRemoveBadROA){
//  for(entry <- memoryImage.single.get.suggestedRoaFilterList.entries) {
//  if(!memoryImage.single.get.filters.entries.contains(new IgnoreFilter(entry.prefix))){
//  entry.block = true
//  memoryImage.single.get.filters.entries += new IgnoreFilter(entry.prefix)
//}
//}
//  lastState =  RoaOperationMode.AutoModeRemoveBadROA
//}
//}
//}