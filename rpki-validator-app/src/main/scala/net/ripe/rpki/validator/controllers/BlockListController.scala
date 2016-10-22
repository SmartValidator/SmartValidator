
package net.ripe.rpki.validator
package controllers

import net.ripe.rpki.validator.lib.Validation._
import net.ripe.rpki.validator.models.{BlockList, _}
import net.ripe.rpki.validator.views.BlockListView

import scalaz.Scalaz._
import scalaz.{Failure, Success}

trait BlockListController extends ApplicationController {
  protected def blockList: BlockList

  protected def addBlockListEntry(entry: BlockFilter): Unit
  protected def removeBlockListEntry(entry: BlockFilter): Unit
  protected def blockListEntryExists(entry: BlockFilter): Boolean = blockList.entries.contains(entry)
  private def baseUrl = views.Tabs.BlockListTab.url


  get(baseUrl) {
    new BlockListView(blockList, messages = feedbackMessages)
  }

  post(baseUrl) {
    submittedBlocker match {
      case Success(entry) =>
        if (blockListEntryExists(entry))
          new BlockListView(blockList, params, Seq(ErrorMessage("filter already exists")))
        else {
          addBlockListEntry(entry)
          redirectWithFeedbackMessages(baseUrl, Seq(SuccessMessage("The prefix has been added to the filters.")))
        }
      case Failure(errors) =>
        new BlockListView(blockList, params, errors)
    }
  }



  private def submittedBlocker: ValidationNEL[FeedbackMessage, BlockFilter] = {
    validateParameter("prefix", required(parseIpPrefix)) map BlockFilter
  }
}

