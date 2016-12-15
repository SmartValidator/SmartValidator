package net.ripe.rpki.validator
package controllers

import grizzled.slf4j.Logging
import net.ripe.ipresource.{Asn, IpRange}
import net.ripe.rpki.validator.bgp.preview.BgpValidatedAnnouncement
import net.ripe.rpki.validator.lib.Validation._
import net.ripe.rpki.validator.models._
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
  protected def blockAsList: BlockAsList

  protected def addBlockAsListEntry(entry: BlockAsFilter): Unit
  protected def removeBlockAsListEntry(entry: BlockAsFilter): Unit
  protected def blockAsListEntryExistsRanking(entry: BlockAsFilter): Boolean = blockAsList.entries.contains(entry)

  get(baseUrl) {
    new RankingView(asRankings, aSrankingSets, getCurrentRtrPrefixes,validatedAnnouncements,blockAsList, messages = feedbackMessages)
  }
  post(baseUrl) {
    submittedBlocker match {
      case Success(entry) =>
        if(blockAsListEntryExistsRanking(new BlockAsFilter(entry.asn,"RankingAsn"))){
          new RankingView(asRankings, aSrankingSets, getCurrentRtrPrefixes,validatedAnnouncements,blockAsList,params, Seq(ErrorMessage("Entry is already blocked")))
        }
        else{
          addBlockAsListEntry(new BlockAsFilter(entry.asn,"RankingAsn"))
          redirectWithFeedbackMessages(baseUrl, Seq(SuccessMessage("The entry has been added to the AS block list.")))
        }
      case Failure(errors) =>
        new RankingView(asRankings, aSrankingSets,getCurrentRtrPrefixes,validatedAnnouncements,blockAsList, params, errors)
    }

  }
  delete(baseUrl) {
    submittedBlocker match {
      case Success(entry) =>
        if(blockAsListEntryExistsRanking(new BlockAsFilter(entry.asn,"RankingAsn"))){
          removeBlockAsListEntry(new BlockAsFilter(entry.asn,"RankingAsn"))
          redirectWithFeedbackMessages(baseUrl, Seq(SuccessMessage("The entry has been removed to the AS block list.")))
        }
        else{
          new RankingView(asRankings, aSrankingSets, getCurrentRtrPrefixes,validatedAnnouncements,blockAsList,params, Seq(ErrorMessage("Entry is already unblocked")))
        }
      case Failure(errors) =>
        new RankingView(asRankings, aSrankingSets,getCurrentRtrPrefixes,validatedAnnouncements,blockAsList, params, errors)
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


}
