/**
  * The BSD License
  *
  * Copyright (c) 2010-2012 RIPE NCC
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *   - Redistributions of source code must retain the above copyright notice,
  * this list of conditions and the following disclaimer.
  *   - Redistributions in binary form must reproduce the above copyright notice,
  * this list of conditions and the following disclaimer in the documentation
  * and/or other materials provided with the distribution.
  *   - Neither the name of the RIPE NCC nor the names of its contributors may be
  * used to endorse or promote products derived from this software without
  * specific prior written permission.
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
package bgp.preview

import grizzled.slf4j.Logging
import net.ripe.ipresource.{Asn, IpRange}
import net.ripe.rpki.validator.RoaBgpIssues.{RoaBgpCollisions, RoaBgpIssue}
import net.ripe.rpki.validator.lib.DateAndTime
import net.ripe.rpki.validator.lib.NumberResources._
import net.ripe.rpki.validator.models.RouteValidity._
import net.ripe.rpki.validator.models.{RouteValidity, RtrPrefix}
import org.joda.time.{DateTime, Period}

import scala.collection.mutable



case class BgpAnnouncementSet(url: String, lastModified: Option[DateTime] = None, entries: Seq[BgpAnnouncement] = Seq.empty)

case class BgpAnnouncement(asn: Asn, prefix: IpRange) {
  def interval = NumberResourceInterval(prefix.getStart, prefix.getEnd)
}

object BgpValidatedAnnouncement {
  def make(announced: BgpAnnouncement, valids: Seq[RtrPrefix] = Seq.empty,
           invalidsAsn: Seq[RtrPrefix] = Seq.empty,
           invalidsLength: Seq[RtrPrefix] = Seq.empty) = BgpValidatedAnnouncement(announced,
    valids.map((Valid, _)) ++ invalidsAsn.map((InvalidAsn, _)) ++ invalidsLength.map((InvalidLength, _)))
}

case class BgpValidatedAnnouncement(announced: BgpAnnouncement, prefixes: Seq[(RouteValidity, RtrPrefix)] = List.empty) {
  require(!invalidsAsn.exists(_.asn == announced.asn), "invalidsAsn must not contain the announced ASN")
  require(!invalidsLength.exists(_.asn != announced.asn), "invalidsLength must only contain VRPs that refer to the same ASN")

  def asn = announced.asn

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

object BgpAnnouncementValidator {
  val VISIBILITY_THRESHOLD = 5

  def validate(announcement: BgpAnnouncement, prefixes: Seq[RtrPrefix]): BgpValidatedAnnouncement =
    validate(announcement, NumberResourceIntervalTree(prefixes: _*))

  def validate(announcement: BgpAnnouncement, prefixTree: NumberResourceIntervalTree[RtrPrefix]): BgpValidatedAnnouncement = {
    val matchingPrefixes = prefixTree.findExactAndAllLessSpecific(announcement.interval)
    val groupedByValidity = matchingPrefixes.map { prefix =>
      val validity = if (hasInvalidAsn(prefix, announcement))
        InvalidAsn
      else if (hasInvalidPrefixLength(prefix, announcement)) InvalidLength else Valid
      (validity, prefix)
    }
    BgpValidatedAnnouncement(announcement, groupedByValidity)
  }

  private def hasInvalidAsn(prefix: RtrPrefix, announced: BgpAnnouncement) =
    prefix.asn != announced.asn

  private def hasInvalidPrefixLength(prefix: RtrPrefix, announced: BgpAnnouncement) =
    prefix.maxPrefixLength.getOrElse(prefix.prefix.getPrefixLength) < announced.prefix.getPrefixLength
}

class BgpAnnouncementValidator(implicit actorSystem: akka.actor.ActorSystem) extends Logging {

  import scala.concurrent.stm._

  private val _validatedAnnouncements = Ref(IndexedSeq.empty[BgpValidatedAnnouncement])
  private val _roaBgpIssueSet = Ref(RoaBgpCollisions(DateTime.now(), IndexedSeq.empty[RoaBgpIssue]))

  def validatedAnnouncements: IndexedSeq[BgpValidatedAnnouncement] = _validatedAnnouncements.single.get

  def roaBgpIssuesSet: RoaBgpCollisions = _roaBgpIssueSet.single.get

  def startUpdate(announcements: Seq[BgpAnnouncement], prefixes: Seq[RtrPrefix]) = {
    val v = validate(announcements, prefixes)
    _validatedAnnouncements.single.set(v)

  }

  def updateRoaBgpConflictsSet(maxRoaBgpConflictDays: Int) ={
    val roaBgpIssuesSet = extractRoaBgpIssues(_validatedAnnouncements.single.get, maxRoaBgpConflictDays)
    _roaBgpIssueSet.single.set(RoaBgpCollisions(DateTime.now(), roaBgpIssuesSet))
  }



  private def validate(announcements: Seq[BgpAnnouncement], prefixes: Seq[RtrPrefix]): IndexedSeq[BgpValidatedAnnouncement] = {
    info("Started validating " + announcements.size + " BGP announcements with " + prefixes.size + " RTR prefixes.")
    val prefixTree = NumberResourceIntervalTree(prefixes: _*)
    val (result, time) = DateAndTime.timed {
      announcements.par.map(BgpAnnouncementValidator.validate(_, prefixTree)).seq.toIndexedSeq
    }
    info(s"Completed validating ${result.size} BGP announcements with ${prefixes.size} RTR prefixes in ${time / 1000.0} seconds")
    result
  }





  def extractRoaBgpIssues(sortedAnnoucments: IndexedSeq[BgpValidatedAnnouncement], maxStaleRecordsStore: Int): IndexedSeq[RoaBgpIssue] = {

    val unemptyCollisionedAnnoucments = sortedAnnoucments.filter(_.prefixes.nonEmpty)
    var collisionsSet: mutable.Set[RoaBgpIssue] = mutable.Set(roaBgpIssuesSet.roaBgpIssuesSet.toArray: _*)
    if (collisionsSet.isEmpty) {
      collisionsSet = mutable.Set[RoaBgpIssue]()
    }

    for (bgpValidatedAnnouncement <- unemptyCollisionedAnnoucments) {
      for (roaBgp <- bgpValidatedAnnouncement.prefixes) {
        if (!roaBgp._1.equals(RouteValidity.Valid)) {
          //TODO: lets filter out those values that were validated for 24 hours
          var foundRoa = collisionsSet.filter(_.roa == roaBgp._2)
          if (foundRoa.nonEmpty)
          {
            //TODO: need to improve performance here
            val foundSimilarAnnouncements = foundRoa.head.bgpAnnouncements.find(x => x._1 == roaBgp._1 && x._2 == bgpValidatedAnnouncement.announced)
            var oldDate: DateTime = DateTime.now()
            if (foundSimilarAnnouncements.nonEmpty) {
              oldDate = foundSimilarAnnouncements.get._3
              foundRoa.head.bgpAnnouncements.remove(foundSimilarAnnouncements.get)
            }
              //The fourth value is the latest date of updating
              foundRoa.head.bgpAnnouncements.add((roaBgp._1, bgpValidatedAnnouncement.announced, oldDate, DateTime.now()))
          }
          else
          {
            val a2 = RoaBgpIssue(roaBgp._2, mutable.Set.empty[(RouteValidity.RouteValidity, BgpAnnouncement, DateTime, DateTime)])
            a2.bgpAnnouncements.add((roaBgp._1, bgpValidatedAnnouncement.announced, DateTime.now(), DateTime.now()))
            collisionsSet.add(a2)
          }
        }
      }
    }

    def isBgpIssueOld(recordValidationDate: DateTime):Boolean = {
      val currentTime = DateTime.now()
      val timeDifference = new Period(currentTime, recordValidationDate)
      if(timeDifference.getDays() < maxStaleRecordsStore ){
        return false
      }
      true
    }

    collisionsSet.foreach(x=> x.bgpAnnouncements --= x.bgpAnnouncements.filter(y => isBgpIssueOld(y._4)))
    collisionsSet = collisionsSet.filterNot(x=> x.bgpAnnouncements.isEmpty)

    //TODO: clean empty anoncments after filteration
    collisionsSet.toIndexedSeq
  }
}
