package net.ripe.rpki.validator.iana.block

import java.net.URL
import java.text.SimpleDateFormat

import grizzled.slf4j.Logging
import net.ripe.ipresource.IpRange

import scala.concurrent._
import scala.xml.XML

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
      blocking {xmlHandler}
    } catch {
      case e: Exception =>
        error("error retrieving IANA entries from " + dump.url, e)
        dump
    }

  }

  protected[block] def makeXmlParser(get: String, dump: IanaAnnouncementSet): IanaAnnouncementSet =
  {

    val ianaRawData = XML.load(new URL(get))
    var ianaRecords = Set[IanaAnnouncement]()
    val dateFormat = new SimpleDateFormat("yyyy-MM")
    (ianaRawData \\"record").foreach{ record =>
      val ip = IpRange.parse((record \\ "prefix").text)
      val designation = (record \\ "designation").text
      val date = dateFormat.parse((record \\ "date").text)
      val status = (record \\ "status").text
      ianaRecords += new IanaAnnouncement(ip, designation, date, status)
    }
    val a = Seq(ianaRecords)

    dump.copy(entries = ianaRecords.toSeq)
  }

}
