
package net.ripe.rpki.validator
package controllers

import net.liftweb.json._
import net.ripe.rpki.validator.RoaBgpIssues.RoaBgpCollisons
import net.ripe.rpki.validator.bgp.preview.{BgpAnnouncementSet, BgpValidatedAnnouncement}
import net.ripe.rpki.validator.models._
import net.ripe.rpki.validator.views.BlockAsListView

trait BlockAsListController extends ApplicationController {
  private def baseUrl = views.Tabs.BlockAsListTab.url

  protected def getRtrPrefixes: Seq[RtrPrefix]
  protected def validatedObjects: ValidatedObjects
  protected def bgpAnnouncementSet: Seq[BgpAnnouncementSet]
  protected def validatedAnnouncements: IndexedSeq[BgpValidatedAnnouncement]
  protected def roaBgpIssuesSet: RoaBgpCollisons
  protected def filters: Filters


  get(baseUrl) {
    new BlockAsListView(messages = feedbackMessages)
  }

  get("/roaIssues.json") {
    import net.liftweb.json.JsonDSL._

    contentType = "text/json"
    response.addHeader("Pragma", "public")
    response.addHeader("Cache-Control", "no-cache")

    val roas = getRtrPrefixes.map(rtr =>
      ("asn" -> rtr.asn.toString) ~
        ("prefix" -> rtr.prefix.toString) ~
        ("maxLength" -> rtr.maxPrefixLength.getOrElse(rtr.prefix.getPrefixLength)) ~
        ("ta" -> rtr.getCaName)
    )

    val validRoaSize = getRtrPrefixes.size
    val bgpAnnoucmentCount = bgpAnnouncementSet.size
    val roaBgpCollisons = roaBgpIssuesSet.roaBgpIssuesSet.size

    response.getWriter.write(compactRender("roas" -> roas))
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

