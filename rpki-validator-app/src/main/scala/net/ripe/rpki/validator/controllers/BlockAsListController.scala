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
package controllers

import net.liftweb.json._
import net.ripe.rpki.validator.RoaBgpIssues.RoaBgpCollisions
import net.ripe.rpki.validator.bgp.preview.{BgpAnnouncementSet, BgpValidatedAnnouncement}
import net.ripe.rpki.validator.models.RouteValidity.RouteValidity
import net.ripe.rpki.validator.models._
import net.ripe.rpki.validator.views.BlockAsListView
import org.joda.time.DateTime
import sun.util.calendar.LocalGregorianCalendar.Date

import scala.collection.immutable.SortedMap
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

trait BlockAsListController extends ApplicationController {
  private def baseUrl = views.Tabs.BlockAsListTab.url

  protected def getRtrPrefixes: Seq[RtrPrefix]
  protected def validatedObjects: ValidatedObjects
  protected def bgpAnnouncementSet: Seq[BgpAnnouncementSet]
  protected def validatedAnnouncements: IndexedSeq[BgpValidatedAnnouncement]
  protected def roaBgpIssuesSet: RoaBgpCollisions
  protected def filters: Filters


  get(baseUrl) {
    new BlockAsListView(messages = feedbackMessages)
  }

  get("/conflitsTimeline.json") {
    import net.liftweb.json.JsonDSL._

    contentType = "text/json"
    response.addHeader("Pragma", "public")
    response.addHeader("Cache-Control", "no-cache")
    var sumInvalidAsn = 0
    val issuesSet = roaBgpIssuesSet.roaBgpIssuesSet
    //TODO delete timelinelist
    var timeLineList = List[Long]()
    roaBgpIssuesSet.roaBgpIssuesSet.toArray.foreach(x=> x.bgpAnnouncements.foreach(y=> timeLineList ++= List(((y._4.toDate.getTime)/10)*10)))
    var timeLineMap = timeLineList.groupBy(identity).mapValues(_.size)
    var sorted = SortedMap[Long, Int]() ++ timeLineMap
    var values =  sorted.valuesIterator.toList
    var labels_long = sorted.keysIterator.toList
    var labels = List[String]()
    labels_long.foreach(x=> labels ++= List((new DateTime(x)).toString()))

    val json = ("labels" -> labels.takeRight(20)) ~ ("series" -> List(values.takeRight(20)))

    response.getWriter.write(pretty(render(json)))
  }


    get("/roaIssues.json") {
      import net.liftweb.json.JsonDSL._

      contentType = "text/json"
      response.addHeader("Pragma", "public")
      response.addHeader("Cache-Control", "no-cache")
      var sumInvalidAsn = 0
      val timeLineList = List()
      roaBgpIssuesSet.roaBgpIssuesSet.toArray.foreach(x=> sumInvalidAsn += x.bgpAnnouncements.count(_._1.equals(RouteValidity.InvalidAsn)))
      var sumInvalidLength = 0
      roaBgpIssuesSet.roaBgpIssuesSet.toArray.foreach(x=> sumInvalidLength += x.bgpAnnouncements.count(_._1.equals(RouteValidity.InvalidLength)))
      val labels = List("Invalid ASN","Invalid Length", "Loose Roa")
      val values = List(sumInvalidAsn,sumInvalidLength,12456)
      val json = ("labels" -> labels) ~ ("series" -> values)

      response.getWriter.write(pretty(render(json)))
    }

  get("/validatedRoasVSfilters.json") {
    import net.liftweb.json.JsonDSL._

    contentType = "text/json"
    response.addHeader("Pragma", "public")
    response.addHeader("Cache-Control", "no-cache")

    val filteredRoas = validatedObjects.getValidatedRtrPrefixes.size - getRtrPrefixes.size
    val validRoaSize = getRtrPrefixes.size

    var labels = List("Total number of ROAs", "Filtered ROAs")
    val values = List(validRoaSize, filteredRoas)
    val json = ("labels" -> labels) ~ ("series" -> values)

    response.getWriter.write(pretty(render(json)))
  }

  get("/bgpAnnouncements.json") {
    import net.liftweb.json.JsonDSL._

    contentType = "text/json"
    response.addHeader("Pragma", "public")
    response.addHeader("Cache-Control", "no-cache")

    val bgpASize = validatedAnnouncements.size
    val validBgpASize = validatedAnnouncements.count(_.validity.equals(RouteValidity.Valid))
    val unknownBgpASize = validatedAnnouncements.count(_.validity.equals(RouteValidity.Unknown))
    val invalidBgpAsize = bgpASize - validBgpASize - unknownBgpASize
    val labels = List("Valid","Invalid", "Unknown")
    val values = List(validBgpASize,invalidBgpAsize,unknownBgpASize )
    val json = ("labels" -> labels) ~ ("series" -> values)

    response.getWriter.write(pretty(render(json)))
  }

}

