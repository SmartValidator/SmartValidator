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
package config

import net.ripe.ipresource.Asn
import net.ripe.rpki.validator.models._
import net.ripe.rpki.validator.util.TrustAnchorLocator

import scala.collection.mutable
import scalaz.Validation

case class MemoryImage(filters: Filters,
                       whitelist: Whitelist,
                       trustAnchors: TrustAnchors,
                       validatedObjects: ValidatedObjects,
                       blockList: BlockList,
                       asRankings: AsRankings,
                       blockAsList: BlockAsList,
                       suggestedRoaFilterList: SuggestedRoaFilterList,
                       pathEndTable: PathEndTable,
                       localPathEndNeighbors: LocalPathEndNeighbors,
                       version: Int = 0) {

  private lazy val distinctRtrPrefixes =
    (Set.empty[RtrPrefix] ++ whitelist.entries ++ filters.filter(validatedObjects.getValidatedRtrPrefixes)).toSeq

  def startProcessingTrustAnchor(locator: TrustAnchorLocator, description: String) =
    copy(trustAnchors = trustAnchors.startProcessing(locator, description))

  def finishedProcessingTrustAnchor(locator: TrustAnchorLocator, result: Validation[String, Seq[ValidatedObject]]) =
    copy(trustAnchors = trustAnchors.finishedProcessing(locator, result))

  def updateValidatedObjects(locator: TrustAnchorLocator, newValidatedObjects: Seq[ValidatedObject]) = {
    trustAnchors.all.find(_.locator == locator) match {
      case Some(trustAnchor) if trustAnchor.enabled =>
        copy(version = version + 1, validatedObjects = validatedObjects.update(locator, newValidatedObjects))
      case _ =>
        this
    }
  }

  def addBlocklistEntry(filter: BlockFilter) = copy(version = version + 1, blockList = blockList.addBlockListEntry(filter))

  def removeBlocklistEntry(filter: BlockFilter) = copy(version = version + 1, blockList = blockList.removeBlockListEntry(filter))

  def addBlockAslistEntry(filter: BlockAsFilter) = copy(version = version + 1, blockAsList = blockAsList.addBlockAsListEntry(filter))

  def removeBlockAslistEntry(filter: BlockAsFilter) = copy(version = version + 1, blockAsList = blockAsList.removeBlockAsListEntry(filter))

  def addWhitelistEntry(entry: RtrPrefix) = copy(version = version + 1, whitelist = whitelist.addEntry(entry))

  def removeWhitelistEntry(entry: RtrPrefix) = copy(version = version + 1, whitelist = whitelist.removeEntry(entry))

  def getDistinctRtrPrefixes: Seq[RtrPrefix] = distinctRtrPrefixes

//  def getRoaIssues: mutable.HashMap[Asn, Boolean] with mutable.SynchronizedMap[Asn, Boolean] = roaIssues

  def addSuggestedRoaFilter(filter: SuggestedRoaFilter) = copy(version = version + 1, suggestedRoaFilterList = suggestedRoaFilterList.addSuggestedRoaFilter(filter))

  def removeSuggestedRoaFilter(filter: SuggestedRoaFilter) = copy(version = version + 1, suggestedRoaFilterList = suggestedRoaFilterList.removeSuggestedRoaFilter(filter))

  def addPathEndNeighbor(entry: Asn) = copy(version = version + 1, localPathEndNeighbors = localPathEndNeighbors.addPathEndNeighbor(entry))

  def removePathEndNeighbor(entry: Asn) = copy(version = version + 1, localPathEndNeighbors = localPathEndNeighbors.removePathEndNeighbor(entry))

  def addPathEndRecord(record: PathEndRecord) = copy(version = version + 1, pathEndTable = pathEndTable.addPathEndRecord(record))

  def removePathEndRecord(record: PathEndRecord) = copy(version = version + 1, pathEndTable = pathEndTable.removePathEndRecord(record))

  def addFilter(filter: IgnoreFilter) = copy(version = version + 1, filters = filters.addFilter(filter))

  def removeFilter(filter: IgnoreFilter) = copy(version = version + 1, filters = filters.removeFilter(filter))

  def updateTrustAnchorState(locator: TrustAnchorLocator, enabled: Boolean) = {
    val newValidatedObjects = enabled match {
      case true => validatedObjects.update(locator, Seq.empty[ValidatedObject])
      case false => validatedObjects.removeTrustAnchor(locator)
    }
    copy(version = version + 1,
      trustAnchors = trustAnchors.updateTrustAnchorState(locator, enabled),
      validatedObjects = newValidatedObjects)
  }

}
