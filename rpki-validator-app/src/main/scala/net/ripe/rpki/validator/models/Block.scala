package net.ripe.rpki.validator
package models

case class BlockList(entries: Set[RtrPrefix] = Set.empty[RtrPrefix]) {
  def addEntry(entry: RtrPrefix) = copy(entries + entry)
  def removeEntry(entry: RtrPrefix) = copy(entries - entry)
}