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
import views.PathEndPreviewView
import net.ripe.ipresource.{Asn, IpRange}
import net.ripe.rpki.validator.bgp.preview.BgpValidatedAnnouncement

trait PathEndController extends ApplicationController {
  protected def pathEndTable: PathEndTable
  protected def localPathEndNeighbors: LocalPathEndNeighbors
  protected def addPathEndRecord(entry: PathEndRecord): Unit
  protected def removePathEndRecord(entry: PathEndRecord): Unit
  protected def addPathEndNeighbor(entry: Asn): Unit
  protected def removePathEndNeighbor(entry: Asn): Unit
  protected def entryExists(entry: PathEndRecord): Boolean = pathEndTable.entries.contains(entry)
  protected def neighborExists(entry: Asn): Boolean = localPathEndNeighbors.entries.contains(entry)
  protected def myAsn: Asn = new Asn(123321)

  protected def validatedAnnouncements: IndexedSeq[BgpValidatedAnnouncement]

  private def baseUrl = views.Tabs.PathEndPreviewTab.url


  get(baseUrl) {
    new PathEndPreviewView(pathEndTable,localPathEndNeighbors,validatedAnnouncements,myAsn,params, messages = feedbackMessages)
  }
  post(baseUrl) {
    submittedEntry match {
      case Success(entry) =>
        if (neighborExists(entry))
          new PathEndPreviewView(pathEndTable,localPathEndNeighbors,validatedAnnouncements,myAsn, params, Seq(ErrorMessage("Asn already defined as a trusted neighbor")))
        else {
          addPathEndNeighbor(entry)
          redirectWithFeedbackMessages(baseUrl, Seq(SuccessMessage("Added new trusted neighbor")))
        }
      case Failure(errors) =>
        new PathEndPreviewView(pathEndTable, localPathEndNeighbors,validatedAnnouncements,myAsn, params, errors)
    }
  }

  delete(baseUrl) {
    submittedEntry match {
      case Success(entry) =>
        if (neighborExists(entry)) {
          removePathEndNeighbor(entry)
          redirectWithFeedbackMessages(baseUrl, Seq(SuccessMessage("The asn removed from trusted neighbors list.")))
        } else {
          new PathEndPreviewView(pathEndTable,localPathEndNeighbors,validatedAnnouncements,myAsn, params, Seq(ErrorMessage("There was a problem removing the Asn from the list.")))
        }
      case Failure(errors) =>
        // go away hacker!
        new PathEndPreviewView(pathEndTable,localPathEndNeighbors,validatedAnnouncements,myAsn, params, errors)
    }
  }
  private def validate(myAsnInput: Asn, asn: Asn): ValidationNEL[FeedbackMessage, Asn] = {
    val validated = asn.success map { _ =>
      new Asn(asn.getValue())

    }

    liftFailErrorMessage(validated)

  }

  private def submittedEntry: ValidationNEL[FeedbackMessage, Asn] = {
    val myAsnInput = validateParameter("myAsn",required(parseAsn))
    val asn = validateParameter("asn", required(parseAsn))

    (myAsnInput |@| asn).apply(validate).flatMap(identity)
  }

}
