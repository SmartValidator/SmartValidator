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
  private def baseUrl = views.Tabs.PrefixWatchListTab.url



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

