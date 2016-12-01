package net.ripe.rpki.validator
package controllers

import grizzled.slf4j.Logging
import net.ripe.ipresource.{IpRange,Asn}
import net.ripe.rpki.validator.bgp.preview.BgpValidatedAnnouncement
import net.ripe.rpki.validator.lib.Validation._
import net.ripe.rpki.validator.models.{AsRankings, BlockFilter, RtrPrefix, ValidatedObjects}
import net.ripe.rpki.validator.ranking.{RankingEntry, RankingSet}
import net.ripe.rpki.validator.views.RankingView

import scalaz.Scalaz.ValidationNEL
import scalaz.Scalaz._
import scalaz.{Failure, Success}

/**
  * Created by fimka on 05/11/16.
  */
trait RankingController extends ApplicationController with Logging {
  protected def asRankings: AsRankings
  protected def validatedObjects: ValidatedObjects
  private def baseUrl = views.Tabs.AsRankingTab.url
  private def getCurrentRtrPrefixes(): Iterable[RtrPrefix] = validatedObjects.getValidatedRtrPrefixes
  protected def aSrankingSets: Seq[RankingSet]
  protected def validatedAnnouncements: Seq[BgpValidatedAnnouncement]

  get(baseUrl) {
    new RankingView(asRankings, aSrankingSets, getCurrentRtrPrefixes,validatedAnnouncements, messages = feedbackMessages)
  }
  post(baseUrl) {
    submittedBlocker match {
      case Success(entry) =>
        val papo = 1
        val papo1 = 2
      case Failure(errors) =>
        val papo = 3
        val papo1 = 4
    }

  }


  private def validate(asn: Asn, name: String, rank: Double): ValidationNEL[FeedbackMessage, RankingEntry] = {

      val validated = asn.success map { _ =>
        new RankingEntry(asn,name, rank)

    }
    liftFailErrorMessage(validated)
  }

  private def submittedBlocker: ValidationNEL[FeedbackMessage, RankingEntry] = {
    val asn = validateParameter("asn", required(parseAsn))
    val name = validateParameter("name", required(parseAsnName))
    val rank = validateParameter("rank", required(parseRank))
    (asn |@| name |@| rank).apply(validate).flatMap(identity)

  }


  //  get(baseUrl) {
//
//  }
//
//  post(baseUrl)
//  {
//
//  }
}
