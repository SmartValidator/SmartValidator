
package net.ripe.rpki.validator
package controllers

import net.ripe.ipresource.{IpRange,Asn}
import net.ripe.rpki.validator.lib.Validation._
import net.ripe.rpki.validator.models.{BlockAsList, _}
import net.ripe.rpki.validator.views.{BlockAsListView, FiltersView}

import scalaz.Scalaz._
import scalaz.{Failure, Success}

trait BlockAsListController extends ApplicationController {
  protected def filters: Filters
  protected def blockAsList: BlockAsList

  protected def addBlockAsListEntry(entry: BlockAsFilter): Unit
  protected def removeBlockAsListEntry(entry: BlockAsFilter): Unit
  protected def blockAsListEntryExists(entry: BlockAsFilter): Boolean = blockAsList.entries.contains(entry)
  private def baseUrl = views.Tabs.BlockAsListTab.url



  get(baseUrl) {
    new BlockAsListView(blockAsList, messages = feedbackMessages)
  }


  delete(baseUrl) {
    submittedBlocker match {
      case Success(entry) =>
        if (blockAsListEntryExists(entry)) {
          removeBlockAsListEntry(entry)
          redirectWithFeedbackMessages(baseUrl, Seq(SuccessMessage("The AS has been removed from the block list.")))
        } else {
          new BlockAsListView(blockAsList, params, Seq(ErrorMessage("As doesn't exist")))
        }
      case Failure(errors) =>
        // go away hacker!
        new BlockAsListView(blockAsList, params, errors)
    }
  }

  private def validate(asn: Asn, origin: String): ValidationNEL[FeedbackMessage, BlockAsFilter] = {
    if (false) {
      ErrorMessage("must be a legal IPv4 or IPv6 prefix", Some("prefix")).failNel
    } else {
      val validated = origin.success map { _ =>
        new BlockAsFilter(asn, origin)
      }
      liftFailErrorMessage(validated)
    }
  }

  private def submittedBlocker: ValidationNEL[FeedbackMessage, BlockAsFilter] = {
    val asn = validateParameter("asn", required(parseAsn))
    val origin = validateParameter("origin", required(parseOrigin))
    (asn |@| origin).apply(validate).flatMap(identity)




  }
}

