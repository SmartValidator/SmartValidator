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
package support

import net.ripe.ipresource.Asn
import net.ripe.rpki.validator.RoaBgpIssues.RoaBgpCollisions
import net.ripe.rpki.validator.api.RestApi
import net.ripe.rpki.validator.config.WebFilter
import net.ripe.rpki.validator.lib.UserPreferences
import net.ripe.rpki.validator.models._
import net.ripe.rpki.validator.ranking.RankingSet
import net.ripe.rpki.validator.util.TrustAnchorLocator
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{GivenWhenThen, Matchers}
import org.scalatra.test.scalatest.ScalatraFeatureSpec

@RunWith(classOf[JUnitRunner])
abstract class FeatureSpecification extends ScalatraFeatureSpec with JunitLoggingSetup with Matchers with MockitoSugar with GivenWhenThen {
  addFilter(new WebFilter {
    override protected def startTrustAnchorValidation(trustAnchors: Seq[String]) = sys.error("TODO")
    override protected def filters = sys.error("TODO")
    override protected def addFilter(filter: IgnoreFilter) = sys.error("TODO")
    override protected def removeFilter(filter: IgnoreFilter) = sys.error("TODO")
    override protected def addSuggestedRoaFilter(filter: SuggestedRoaFilter) = sys.error("TODO")
    override protected def removeSuggestedRoaFilter(filter: SuggestedRoaFilter) = sys.error("TODO")
    override protected def addPathEndRecord(filter: PathEndRecord) = sys.error("TODO")
    override protected def removePathEndRecord(filter: PathEndRecord) = sys.error("TODO")
    override protected def addPathEndNeighbor(entry: Asn) = sys.error("TODO")
    override protected def removePathEndNeighbor(entry: Asn) = sys.error("TODO")
    override protected def whitelist = sys.error("TODO")
    override protected def addWhitelistEntry(entry: RtrPrefix) = sys.error("TODO")
    override protected def removeWhitelistEntry(entry: RtrPrefix) = sys.error("TODO")
    override protected def updateFilters(forceUpdate: Boolean = false) = sys.error("TODO")
    override protected def blockList: BlockList = sys.error("TODO")
    override protected def blockAsList: BlockAsList = sys.error("TODO")
    override protected def addBlockListEntry(entry: BlockFilter) = sys.error("TODO")
    override protected def removeBlockListEntry(entry: BlockFilter) = sys.error("TODO")
    override protected def addBlockAsListEntry(entry: BlockAsFilter) = sys.error("TODO")
    override protected def removeBlockAsListEntry(entry: BlockAsFilter) = sys.error("TODO")
    override protected def bgpAnnouncementSet = sys.error("TODO")
    override protected def validatedAnnouncements = sys.error("TODO")
    override protected def validatedIanaSets = sys.error("TODO")
    override protected def getRtrPrefixes = sys.error("TODO")
    override protected def suggestedWhitelistASN = sys.error("TBD")
    override protected def timelineConflicts = sys.error("TBD")
    override protected def sessionData = sys.error("TBD")
    override lazy val trustAnchors = sys.error("TBD")
    override lazy val validatedObjects = sys.error("TBD")

    override def userPreferences = UserPreferences(updateAlertActive = false)
    override def newVersionDetailFetcher = sys.error("TODO")
    override def updateUserPreferences(userPreferences: UserPreferences) = sys.error("TODO")
    override protected def updateTrustAnchorState(locator: TrustAnchorLocator, enabled: Boolean) {}

    override protected def asRankings: AsRankings = sys.error("TODO")
    override protected def aSrankingSets: Seq[RankingSet] = sys.error("TODO")

    override protected def suggestedRoaFilters: SuggestedRoaFilterList = sys.error("TODO")
    override protected def pathEndTable: PathEndTable = sys.error("TODO")
    override protected def localPathEndNeighbors: LocalPathEndNeighbors = sys.error("TODO")


    override protected def roaBgpIssuesSet: RoaBgpCollisions = sys.error("TODO")
  }, "/*")

  addServlet(new RestApi {
    protected def getVrpObjects = ???
    protected def getCachedObjects = ???
  }, "/api/*")
}
