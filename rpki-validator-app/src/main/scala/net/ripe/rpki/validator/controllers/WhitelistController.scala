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

import scalaz._
import Scalaz._
import lib.Validation._
import models._
import net.ripe.rpki.validator.RoaBgpIssues.{RoaBgpCollisions, RoaBgpIssue}
import views.WhitelistView
import net.ripe.rpki.validator.bgp.preview.BgpValidatedAnnouncement
import net.ripe.rpki.validator.lib.UserPreferences
import org.joda.time.{DateTime, Period}

trait WhitelistController extends ApplicationController {
  protected def whitelist: Whitelist
  protected def addWhitelistEntry(entry: RtrPrefix): Unit
  protected def removeWhitelistEntry(entry: RtrPrefix): Unit
  protected def entryExists(entry: RtrPrefix): Boolean = whitelist.entries.contains(entry)
  protected def userPreferences: UserPreferences
  protected def validatedAnnouncements: Seq[BgpValidatedAnnouncement]

  private def baseUrl = views.Tabs.WhitelistTab.url


  protected def roaBgpIssuesSet: RoaBgpCollisions

  get(baseUrl) {
    new WhitelistView(whitelist, validatedAnnouncements, getBGPConflictsFiltered(),messages = feedbackMessages)
  }

  post(baseUrl) {
    submittedEntry match {
      case Success(entry) =>
        if (entryExists(entry))
          new WhitelistView(whitelist, validatedAnnouncements, getBGPConflictsFiltered(), params, Seq(ErrorMessage("entry already exists in the whitelist")))
        else {
          addWhitelistEntry(entry)
          redirectWithFeedbackMessages(baseUrl, Seq(SuccessMessage("The entry has been added to the whitelist.")))
        }
      case Failure(errors) =>
        new WhitelistView(whitelist, validatedAnnouncements, getBGPConflictsFiltered(), params, errors)
    }
  }

  delete(baseUrl) {
    submittedEntry match {
      case Success(entry) =>
        if (entryExists(entry)) {
          removeWhitelistEntry(entry)
          redirectWithFeedbackMessages(baseUrl, Seq(SuccessMessage("The entry has been removed from the whitelist.")))
        } else {
          new WhitelistView(whitelist, validatedAnnouncements, getBGPConflictsFiltered(), params, Seq(ErrorMessage("entry no longer exists in the whitelist")))
        }
      case Failure(errors) =>
        // go away hacker!
        new WhitelistView(whitelist, validatedAnnouncements, getBGPConflictsFiltered(), params, errors)
    }
  }

  private def submittedEntry: ValidationNEL[FeedbackMessage, RtrPrefix] = {
    val asn = validateParameter("asn", required(parseAsn))
    val prefix = validateParameter("prefix", required(parseIpPrefix))
    val maxPrefixLength = validateParameter("maxPrefixLength", optional(parseInt))

    (asn |@| prefix |@| maxPrefixLength).apply(RtrPrefix.validate).flatMap(identity)
  }
  def isBgpIssueOld(recordValidationDate: DateTime):Boolean = {
    val currentTime = DateTime.now()
    val timeDifference = new Period(currentTime, recordValidationDate)
    if(timeDifference.getDays() >= userPreferences.conflictCertDays ){
      return false
    }
    true
  }
  private def getBGPConflictsFiltered(): IndexedSeq[RoaBgpIssue]={
    if(userPreferences.roaBgpConflictLearnMode){
      val safeDays = userPreferences.conflictCertDays
      roaBgpIssuesSet.roaBgpIssuesSet.foreach(x=> x.bgpAnnouncements --= x.bgpAnnouncements.filter(y => isBgpIssueOld(y._3)))
      roaBgpIssuesSet.roaBgpIssuesSet.filterNot(x=> x.bgpAnnouncements.isEmpty)
    }
    roaBgpIssuesSet.roaBgpIssuesSet
  }

}
