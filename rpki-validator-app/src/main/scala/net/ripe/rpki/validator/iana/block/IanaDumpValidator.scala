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
package net.ripe.rpki.validator.iana.block

import java.util.Date

import grizzled.slf4j.Logging
import net.ripe.ipresource.IpRange
import net.ripe.rpki.validator.models.RouteValidity._
import net.ripe.rpki.validator.models.{RouteValidity, RtrPrefix}


//TODO: currently copied fully from bgpDumpValidator.scala
case class IanaAnnouncementSet(url: String, entries: Seq[IanaAnnouncement] = Seq.empty)

case class IanaAnnouncement(prefix: IpRange, designation: String, date: Date, status: String) {
}

object IanaValidatedAnnouncement {
  def make(announced: IanaAnnouncement, valids: Seq[RtrPrefix] = Seq.empty,
           invalidsAsn: Seq[RtrPrefix] = Seq.empty,
           invalidsLength: Seq[RtrPrefix] = Seq.empty) = IanaValidatedAnnouncement(announced,
    valids.map((Valid, _)) ++ invalidsAsn.map((InvalidAsn, _)) ++ invalidsLength.map((InvalidLength, _)))
}


case class IanaValidatedAnnouncement(announced: IanaAnnouncement, prefixes: Seq[(RouteValidity, RtrPrefix)] = List.empty) {
//  require(!invalidsAsn.exists(_.asn == announced.asn), "invalidsAsn must not contain the announced ASN")
//  require(!invalidsLength.exists(_.asn != announced.asn), "invalidsLength must only contain VRPs that refer to the same ASN")

  def prefix = announced.prefix

  def validity = {
    if (valids.nonEmpty) RouteValidity.Valid
    else if (invalidsLength.nonEmpty) RouteValidity.InvalidLength
    else if (invalidsAsn.nonEmpty) RouteValidity.InvalidAsn
    else RouteValidity.Unknown
  }

  def valids = prefixes.collect { case (Valid, p) => p }

  def invalidsAsn = prefixes.collect { case (InvalidAsn, p) => p }

  def invalidsLength = prefixes.collect { case (InvalidLength, p) => p }
}

object IanaAnnouncementValidator {
//  val VISIBILITY_THRESHOLD = 5
//
//  def validate(announcement: IanaAnnouncement, prefixes: Seq[RtrPrefix]): IanaValidatedAnnouncement =
//    validate(announcement, NumberResourceIntervalTree(prefixes: _*))

//  def validate(announcement: IanaAnnouncement, prefixTree: NumberResourceIntervalTree[RtrPrefix]): IanaValidatedAnnouncement = {
////    val matchingPrefixes = prefixTree.findExactAndAllLessSpecific(announcement.interval)
////    val groupedByValidity = matchingPrefixes.map { prefix =>
////      val validity = if (hasInvalidAsn(prefix, announcement))
////        InvalidAsn
////      else if (hasInvalidPrefixLength(prefix, announcement)) InvalidLength else Valid
////      (validity, prefix)
////    }
////    IanaValidatedAnnouncement(announcement, groupedByValidity)
//  }

//  private def hasInvalidAsn(prefix: RtrPrefix, announced: IanaAnnouncement) =
//    prefix.asn != announced.asn

//  private def hasInvalidPrefixLength(prefix: RtrPrefix, announced: IanaAnnouncement) =
//    prefix.maxPrefixLength.getOrElse(prefix.prefix.getPrefixLength) < announced.prefix.getPrefixLength
}

class IanaAnnouncementValidator(implicit actorSystem: akka.actor.ActorSystem) extends Logging {

//  import scala.concurrent.stm._
//
//  private val _validatedAnnouncements = Ref(IndexedSeq.empty[IanaValidatedAnnouncement])
//
//  def validatedAnnouncements: IndexedSeq[IanaValidatedAnnouncement] = _validatedAnnouncements.single.get
//
//  def startUpdate(announcements: Seq[IanaAnnouncement], prefixes: Seq[RtrPrefix]) = {
//    val v = validate(announcements, prefixes)
//    _validatedAnnouncements.single.set(v)
//  }
//
//  private def validate(announcements: Seq[IanaAnnouncement], prefixes: Seq[RtrPrefix]): IndexedSeq[IanaValidatedAnnouncement] = {
//    info("Started validating " + announcements.size + " Iana announcements with " + prefixes.size + " RTR prefixes.")
//    val prefixTree = NumberResourceIntervalTree(prefixes: _*)
//    val (result, time) = DateAndTime.timed {
//      announcements.par.map(IanaAnnouncementValidator.validate(_, prefixTree)).seq.toIndexedSeq
//    }
//    info(s"Completed validating ${result.size} Iana announcements with ${prefixes.size} RTR prefixes in ${time / 1000.0} seconds")
//    result
//  }
}