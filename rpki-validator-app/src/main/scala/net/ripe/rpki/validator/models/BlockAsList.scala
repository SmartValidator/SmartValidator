
package net.ripe.rpki.validator
package models

import net.ripe.ipresource.{Asn, IpRange}
import net.ripe.rpki.validator.lib.Validation._

import scalaz.Scalaz._


case class BlockAsFilter(asn: Asn, origin: String = "Manual") {

}

case class BlockAsList(entries: Set[BlockAsFilter] = Set.empty) {
  def addBlockAsListEntry(entry: BlockAsFilter) = copy(entries + entry)
  def removeBlockAsListEntry(entry: BlockAsFilter) = copy(entries - entry)


}
