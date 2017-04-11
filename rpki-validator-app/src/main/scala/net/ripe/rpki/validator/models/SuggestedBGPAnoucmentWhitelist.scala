package net.ripe.rpki.validator.models

import net.ripe.ipresource.{Asn, IpRange}

/**
  * Created by fimka on 08/12/16.
  */
case class SuggestedWhitelistEntry(asn: Asn, prefix: IpRange, maxLength: Integer, var block: Boolean = false, var fix: Boolean = false) {

}

case class SuggestedWhitelistEntryList(var entries: scala.collection.mutable.Set[SuggestedWhitelistEntry] = scala.collection.mutable.Set.empty) {
  def addSuggestedWhitelistEntry(entry: SuggestedWhitelistEntry) = copy(entries + entry)
  def removeSuggestedWhitelistEntry(entry: SuggestedWhitelistEntry) = copy(entries - entry)

}