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
