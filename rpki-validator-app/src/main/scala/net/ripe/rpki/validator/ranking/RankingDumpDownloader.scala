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
package net.ripe.rpki.validator.ranking

import javax.servlet.http.HttpServletResponse.SC_OK

import grizzled.slf4j.Logging
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.{HttpClient, ResponseHandler}
import org.apache.http.entity.StringEntity

//

import scala.concurrent.{ExecutionContext, Future, blocking}

class RankingDumpDownloader(httpClient: HttpClient) extends Logging {
  val DEFAULT_URLS = Seq(
    "http://bgpranking.circl.lu/json")

  /**
    * Refreshes the given rankingDump. If the source information was not modified or could not be retrieved the input dump is returned.
    */
  def download(dump: AsRankingSet)(implicit ec: ExecutionContext): Future[AsRankingSet] = Future {
    try {
      val post = new HttpPost("http://bgpranking.circl.lu/json")
//      post.addHeader("content-type", "application/json")
      val postString = new StringEntity("{\"method\": \"cached_top_asns\", \"source\": \"global\", \"date\":\"\" , \"limit\":\"100\", \"with_sources\":\"False\" }")
      post.setEntity(postString)
      post.setHeader("Content-type", "application/json")
      val responseHandler = makeResponseHandler(dump)

      blocking { httpClient.execute(post, responseHandler) }
    } catch {
      case e: Exception =>
        error("error retrieving BGP entries from " + dump.url, e)
        dump
    }
  }


  protected def makeResponseHandler(dump: AsRankingSet): ResponseHandler[AsRankingSet] = {
    val responseHandler = new ResponseHandler[AsRankingSet]() {
      override def handleResponse(response: HttpResponse): AsRankingSet = {
        response.getStatusLine.getStatusCode match {
          case SC_OK =>
            try {
              RankingDump.parseRank(response.getEntity.getContent) match {
                case Left(exception) =>
//                  error("Error parsing ranking entries." + exception.toString, exception)
                  dump
                case Right(entries) =>
////                  val modified = lastModified(response)
////                  info("Retrieved " + entries.size + " entries from " + dump.url + ", last modified at " + modified.getOrElse("unknown"))
                  dump
              }
            } catch {
              case exception: Exception =>
                error("Error parsing ranking entries . " + exception.toString, exception)
                dump
            }
//          case SC_NOT_MODIFIED if dump.lastModified.isDefined =>
//            EntityUtils.consume(response.getEntity)
//            info("BGP entries from " + dump.url + " were not modified since " + dump.lastModified.get)
//            dump
//          case _ =>
//            EntityUtils.consume(response.getEntity)
//            warn("error retrieving BGP entries from " + dump.url + ". Code: " + response.getStatusLine.getStatusCode + " " + response.getStatusLine.getReasonPhrase)
//            dump
        }
      }
    }
    responseHandler
  }

}