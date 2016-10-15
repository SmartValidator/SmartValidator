package net.ripe.rpki.validator
package models

case class BlockList(entries: Set[RtrPrefix] = Set.empty[RtrPrefix]) {
  def addBlockListEntry(entry: RtrPrefix) = copy(entries + entry)
  def removeBlockListEntry(entry: RtrPrefix) = copy(entries - entry)
}