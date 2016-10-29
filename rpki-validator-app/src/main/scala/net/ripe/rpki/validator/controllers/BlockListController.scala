
package net.ripe.rpki.validator
package controllers

import net.ripe.ipresource.IpRange
import net.ripe.rpki.validator.iana.block.IanaAnnouncement
import net.ripe.rpki.validator.iana.block.IanaAnnouncementSet
import net.ripe.rpki.validator.lib.Validation._
import net.ripe.rpki.validator.models.{BlockList, _}
import net.ripe.rpki.validator.views.{BlockListView, FiltersView}

import scalaz.Scalaz._
import scalaz.{Failure, Success}

trait BlockListController extends ApplicationController {
  protected def filters: Filters
  protected def blockList: BlockList
  protected def validatedIanaSets: Seq[IanaAnnouncementSet]
  protected def validatedIanaBlockFilter: Set[BlockFilter] = {var returnSet: Set[BlockFilter] = Set()
  val a = validatedIanaSets(0).entries
    for (entry <- a) {
      returnSet += new BlockFilter(entry.prefix, "IanaReserved")
    }
    returnSet
  }
  protected def addBlockListEntry(entry: BlockFilter): Unit
  protected def removeBlockListEntry(entry: BlockFilter): Unit
  protected def blockListEntryExists(entry: BlockFilter): Boolean = blockList.entries.contains(entry)
  private def baseUrl = views.Tabs.BlockListTab.url



  get(baseUrl) {
    new BlockListView(blockList,validatedIanaBlockFilter, messages = feedbackMessages)
  }

  post(baseUrl) {
    submittedBlocker match {
      case Success(entry) =>
        if (blockListEntryExists(entry))
          new BlockListView(blockList, validatedIanaBlockFilter,params, Seq(ErrorMessage("filter already exists")))
        else {
          addBlockListEntry(entry)
          redirectWithFeedbackMessages(baseUrl, Seq(SuccessMessage("The prefix has been added to the filters.")))
        }
      case Failure(errors) =>
       new BlockListView(blockList,validatedIanaBlockFilter, params, errors)
    }
  }

  delete(baseUrl) {
    submittedBlocker match {
      case Success(entry) =>
        if (blockListEntryExists(entry)) {
          removeBlockListEntry(entry)
          redirectWithFeedbackMessages(baseUrl, Seq(SuccessMessage("The prefix has been removed from the filters.")))
        } else {
          new BlockListView(blockList, validatedIanaBlockFilter, params, Seq(ErrorMessage("Block list filter doesn't exist")))
        }
      case Failure(errors) =>
        // go away hacker!
        new BlockListView(blockList, validatedIanaBlockFilter, params, errors)
    }
  }

 private def validate(prefix: IpRange, origin: String): ValidationNEL[FeedbackMessage, BlockFilter] = {
    if (!prefix.isLegalPrefix) {
      ErrorMessage("must be a legal IPv4 or IPv6 prefix", Some("prefix")).failNel
    } else {
      val validated = origin.success map { _ =>
        new BlockFilter(prefix, origin)
      }
      liftFailErrorMessage(validated)
    }
  }

  private def submittedBlocker: ValidationNEL[FeedbackMessage, BlockFilter] = {
    val prefix = validateParameter("prefix", required(parseIpPrefix))
    val origin = validateParameter("origin", required(parseOrigin))
    (prefix |@| origin).apply(validate).flatMap(identity)




  }
}

