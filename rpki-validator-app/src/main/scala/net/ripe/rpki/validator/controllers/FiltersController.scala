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

import net.ripe.ipresource.{Asn, IpRange}
import net.ripe.rpki.validator.RoaBgpIssues._
import net.ripe.rpki.validator.lib.RoaOperationMode._
import net.ripe.rpki.validator.lib.UserPreferences
import net.ripe.rpki.validator.lib.Validation._
import net.ripe.rpki.validator.models._
import net.ripe.rpki.validator.views.FiltersView

import scalaz.Scalaz._
import scalaz._

trait FiltersController extends ApplicationController {
  protected def updateFilters(forceUpdate: Boolean = false)
  protected def filters: Filters
  protected def roaBgpIssuesSet: RoaBgpCollisions
  protected def suggestedRoaFilters : SuggestedRoaFilterList
  protected def addSuggestedRoaFilter(filter: SuggestedRoaFilter): Unit
  protected def removeSuggestedRoaFilter(filter: SuggestedRoaFilter): Unit
  protected def addFilter(filter: IgnoreFilter): Unit
  protected def removeFilter(filter: IgnoreFilter): Unit
  protected def filterExists(filter: IgnoreFilter): Boolean = filters.entries.contains(filter)
  protected def validatedObjects: ValidatedObjects
  protected def userPreferences: UserPreferences
  protected var lastState: RoaOperationMode = null;

  private def baseUrl = views.Tabs.FiltersTab.url
  
  private def getCurrentRtrPrefixes(): Iterable[RtrPrefix] = validatedObjects.getValidatedRtrPrefixes

  get(baseUrl) {
//    updateFilters(false)
    new FiltersView(filters, getCurrentRtrPrefixes,suggestedRoaFilters, messages = feedbackMessages)
  }

  post(baseUrl) {
    submitedSuggestedRoaFilter match {
      case Success(entry) =>
        if (filterExists(new IgnoreFilter(entry.prefix,entry.affectedRoas)))
          new FiltersView(filters, getCurrentRtrPrefixes,suggestedRoaFilters, params, Seq(ErrorMessage("filter already exists")))
        else {
          addFilter(new IgnoreFilter(entry.prefix))
          redirectWithFeedbackMessages(baseUrl, Seq(SuccessMessage("The prefix has been added to the filters.")))
        }
      case Failure(errors) =>
        new FiltersView(filters, getCurrentRtrPrefixes,suggestedRoaFilters, params, errors)
    }

  }

  delete(baseUrl) {
    submittedFilter match {
      case Success(entry) =>
        val filter = IgnoreFilter(entry)
        if (filterExists(filter)) {
          removeFilter(filter)
          redirectWithFeedbackMessages(baseUrl, Seq(SuccessMessage("The prefix has been removed from the filters.")))
        } else {
          new FiltersView(filters, getCurrentRtrPrefixes,suggestedRoaFilters, params, Seq(ErrorMessage("filter no longer exists")))
        }
      case Failure(errors) =>
        // go away hacker!
        new FiltersView(filters, getCurrentRtrPrefixes,suggestedRoaFilters, params, errors)
    }
  }

  private def submittedFilter: ValidationNEL[FeedbackMessage, IpRange] = {
    val prefix = validateParameter("prefix", required(parseIpPrefix))
    prefix
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

  private def submitedSuggestedRoaFilter: ValidationNEL[FeedbackMessage, SuggestedRoaFilter] = {
    val asn = validateParameter("asn", required(parseAsn))
    val prefix = validateParameter("prefix", required(parseIpPrefix))
    val maxLength = validateParameter("maxLength", required(parseInt))
    val block = validateParameter("block", required(parseBool))
    val fix = validateParameter("fix", required(parseBool))
    (asn |@| prefix |@| maxLength |@| block |@| fix).apply(validate).flatMap(identity)
  }

}
