package net.ripe.rpki.validator.iana.block

import java.net.URL
import java.text.SimpleDateFormat

import grizzled.slf4j.Logging
import net.ripe.ipresource.IpRange

import scala.concurrent._

/**
  * Created by fimka on 16/10/16.
  */
class IanaDumpDownloader() extends Logging {

  /**
    * Refreshes the given IanaRisDump. If the source information was not modified or could not be retrieved the input
    * dump is returned.
    */
  def download(dump: IanaAnnouncementSet)(implicit ec: ExecutionContext): Future[IanaAnnouncementSet] = Future {
    try {

      val xmlHandler = makeXmlParser(dump.url, dump)
      xmlHandler
    } catch {
      case e: Exception =>
        error("error retrieving IANA entries from " + dump.url, e)
        dump
    }

  }

  protected[block] def makeXmlParser(get: String, dump: IanaAnnouncementSet): IanaAnnouncementSet =
  {

    var ianaRawData = scala.xml.XML.load(new URL(get))
    val ianaRecords = Set[IanaAnnouncement]()
    val recordRoot = (ianaRawData \\ "registry")
    for(record <- (recordRoot \\ "record")){
      ianaRecords + new IanaAnnouncement(IpRange.parse("10.0.0.0/8"), (record \\ "designation").text, new SimpleDateFormat(),  (record \\ "status").text)
    }
    dump
  }

}
