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


    dump
  }


//  protected[preview] def makeResponseHandler(dump: IanaAnnouncementSet): ResponseHandler[IanaAnnouncementSet] = {
//    val responseHandler = new ResponseHandler[IanaAnnouncementSet]() {
//      override def handleResponse(response: HttpResponse): IanaAnnouncementSet = {
//        response.getStatusLine.getStatusCode match {
//          case SC_OK =>
//            try {
//              IanaDump.parse(new InputStream(response.getEntity.getContent)) match {
//                case Left(exception) =>
//                  error("Error parsing Iana entries from " + dump.url + ". " + exception.toString, exception)
//                  dump
//                case Right(entries) =>
//                  val modified = lastModified(response)
//                  info("Retrieved " + entries.size + " entries from " + dump.url + ", last modified at " +
//                    modified.getOrElse("unknown"))
//                  dump.copy(lastModified = modified, entries = IanaRisDump.toAnnouncedRoutes(entries))
//              }
//            } catch {
//              case exception: Exception =>
//                error("Error parsing Iana entries from " + dump.url + ". " + exception.toString, exception)
//                dump
//            }
//          case SC_NOT_MODIFIED if dump.lastModified.isDefined =>
//            EntityUtils.consume(response.getEntity)
//            info("Iana entries from " + dump.url + " were not modified since " + dump.lastModified.get)
//            dump
//          case _ =>
//            EntityUtils.consume(response.getEntity)
//            warn("error retrieving Iana entries from " + dump.url + ". Code: " + response.getStatusLine.getStatusCode + " " + response.getStatusLine.getReasonPhrase)
//            dump
//        }
//      }
//    }
//    responseHandler
//  }

}
