package net.ripe.rpki.validator.iana.block

import java.net.URL

import grizzled.slf4j.Logging

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
      blocking{ xmlHandler}
    } catch {
      case e: Exception =>
        error("error retrieving IANA entries from " + dump.url, e)
        dump
    }

  }

  protected[preview] def makeXmlParser(get: String, dump: IanaAnnouncementSet): IanaAnnouncementSet =
  {

    var a = scala.xml.XML.load(new URL(get))
    val recordRoot = (a \\ "registry")
    for(record <- recordRoot \\ "record"){
      //ex
    }

  }

}
