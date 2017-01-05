package net.ripe.rpki.validator.controllers

import grizzled.slf4j.Logging
import net.ripe.ipresource.{Asn, IpRange}
import net.ripe.rpki.validator.bgp.preview.BgpValidatedAnnouncement
import net.ripe.rpki.validator.lib.Validation._
import net.ripe.rpki.validator.models._
import net.ripe.rpki.validator.ranking.RankingEntry
import net.ripe.rpki.validator.views
import net.ripe.rpki.validator.views.{RankingView, RoaBlackListView}

import scala.collection.mutable.ArrayBuffer
import scalaz.Scalaz._
import scalaz.{Failure, Success}

/**
  * Created by fimka on 08/12/16.
  */
trait SuggestedRoaFiltersController extends ApplicationController with Logging {
  protected def suggestedRoaFilters: SuggestedRoaFilterList
  private def baseUrl = views.Tabs.RoaBlackList.url
  protected def validatedObjects: ValidatedObjects
  private def getCurrentRtrPrefixes(): Iterable[RtrPrefix] = validatedObjects.getValidatedRtrPrefixes
  protected def validatedAnnouncements: IndexedSeq[BgpValidatedAnnouncement]
  protected var looseRoas = findLooseROA()


  private def isRoaLoose(roa: RtrPrefix): Boolean = {
    var maxAdvertisiedLength:Int = 0
    var roaMaxLength : Int = roa.prefix.getPrefixLength
    if(roa.maxPrefixLength.isDefined){
      roaMaxLength = roa.maxPrefixLength.get
    }

    for(bgpAnnouncement <- validatedAnnouncements){
      if(roa.prefix.contains(bgpAnnouncement.prefix)) // change to think on maxLength too
      {
        if(maxAdvertisiedLength < bgpAnnouncement.prefix.getPrefixLength){
          maxAdvertisiedLength = bgpAnnouncement.prefix.getPrefixLength
        }

        if(maxAdvertisiedLength == roaMaxLength){
          return true
        }
      }
    }

    false
  }

  private def findLooseROA() : ArrayBuffer[LooseRoa] = {
    var looseRoas = ArrayBuffer[LooseRoa]()
    val currentRoas = getCurrentRtrPrefixes()
    currentRoas.foreach{ roa =>
          if(isRoaLoose(roa)){
            val temp = LooseRoa(roa.asn, roa.prefix, roa.maxPrefixLength.getOrElse(roa.prefix.getPrefixLength))
//              val temp = LooseRoa(roa.asn, roa.prefix, roa.maxPrefixLength.get)

            looseRoas += temp
          }
        }
    looseRoas
  }

  get(baseUrl){
    new RoaBlackListView(suggestedRoaFilters, findLooseROA(), messages = feedbackMessages)
  }


  post(baseUrl) {
//    submittedBlocker match {
//      case Success(entry) =>
//        if(blockAsListEntryExistsRanking(new BlockAsFilter(entry.asn,"RankingAsn"))){
//          new RankingView(asRankings, aSrankingSets, getCurrentRtrPrefixes,validatedAnnouncements,blockAsList,params, Seq(ErrorMessage("Entry is already blocked")))
//        }
//        else{
//          addBlockAsListEntry(new BlockAsFilter(entry.asn,"RankingAsn"))
//          redirectWithFeedbackMessages(baseUrl, Seq(SuccessMessage("The entry has been added to the AS block list.")))
//        }
//      case Failure(errors) =>
//        new RankingView(asRankings, aSrankingSets,getCurrentRtrPrefixes,validatedAnnouncements,blockAsList, params, errors)
//    }

  }



  private def validate(asn: Asn, prefix: IpRange, maxLength: Int, block: Boolean, fix: Boolean): ValidationNEL[FeedbackMessage, SuggestedRoaFilter] = {
    if (!prefix.isLegalPrefix) {
      ErrorMessage("must be a legal IPv4 or IPv6 prefix", Some("prefix")).failNel
    } else {
      val validated = asn.success map { _ =>
        new SuggestedRoaFilter(asn, prefix, maxLength, block, fix)

      }

      liftFailErrorMessage(validated)
    }
  }

  private def submittedBlocker: ValidationNEL[FeedbackMessage, SuggestedRoaFilter] = {
    val asn = validateParameter("asn", required(parseAsn))
    val prefix = validateParameter("prefix", required(parseIpPrefix))
    val maxLength = validateParameter("maxlength", required(parseInt))
    val block = validateParameter("block", required(parseBool))
    val fix = validateParameter("fix", required(parseBool))
    (asn |@| prefix |@| maxLength |@| block |@| fix).apply(validate).flatMap(identity)


  }


}
