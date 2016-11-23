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
  def download(dump: RankingSet)(implicit ec: ExecutionContext): Future[RankingSet] = Future {
    try {
      val post = makePostExecutor(dump)
      val responseHandler = makeResponseHandler(dump)
      blocking { httpClient.execute(post, responseHandler) }
    } catch {
      case e: Exception =>
        error("error retrieving BGP entries from " + dump.url, e)
        dump
    }
  }


  protected def makePostExecutor(dump: RankingSet): HttpPost = {
    val postRequest = new HttpPost(dump.url)
    val postString = new StringEntity("{\"method\": \"cached_top_asns\", \"source\": \"global\", \"limit\":\"100\", \"with_sources\":\"False\" }")
    postRequest.setEntity(postString)
    postRequest.setHeader("Content-type", "application/json")
    postRequest
  }


  protected def makeResponseHandler(dump: RankingSet): ResponseHandler[RankingSet] = {
    val responseHandler = new ResponseHandler[RankingSet]() {
      override def handleResponse(response: HttpResponse): RankingSet = {
        response.getStatusLine.getStatusCode match {
          case SC_OK =>
            try {
              val dump1 = RankingDump.parseRank(response.getEntity.getContent, dump)
              dump.copy(url = dump1.url, source = dump1.source, lastTotal = dump1.lastTotal, lastModified = dump1.lastModified, entries = dump1.entries)
              }
            catch {
              case exception: Exception =>
                error("Error parsing ranking entries . " + exception.toString, exception)
                dump
            }
        }
      }
    }
    responseHandler
  }
}