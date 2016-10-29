
package net.ripe.rpki.validator
package models

import net.ripe.ipresource.{Asn, IpRange}
import net.ripe.rpki.validator.lib.Validation._

import scalaz.Scalaz._


case class BlockFilter(prefix: IpRange, origin: String = "Manual") {

  def shouldBlock(rtrPrefix: RtrPrefix): Boolean = prefix.overlaps(rtrPrefix.prefix)
}

case class BlockList(entries: Set[BlockFilter] = Set.empty) {
  def addBlockListEntry(entry: BlockFilter) = copy(entries + entry)
  def removeBlockListEntry(entry: BlockFilter) = copy(entries - entry)

  def filter(input: Iterable[RtrPrefix]): Iterable[RtrPrefix] =
    if (entries.isEmpty) input
    else input.filterNot(shouldBlock)

  private def shouldBlock(rtrPrefix: RtrPrefix) = entries.exists(_.shouldBlock(rtrPrefix))

}
