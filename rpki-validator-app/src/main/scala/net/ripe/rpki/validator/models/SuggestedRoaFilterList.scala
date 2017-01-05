package net.ripe.rpki.validator
package models


import net.ripe.ipresource.{Asn, IpRange}

/**
  * Created by fimka on 08/12/16.
  */
  case class SuggestedRoaFilter(asn: Asn, prefix: IpRange, maxLength: Integer, var block: Boolean = false, var fix: Boolean = false) {

  }

  case class SuggestedRoaFilterList(var entries: scala.collection.mutable.Set[SuggestedRoaFilter] = scala.collection.mutable.Set.empty) {
    def addSuggestedRoaFilter(filter: SuggestedRoaFilter) = copy(entries + filter)
    def removeSuggestedRoaFilter(filter: SuggestedRoaFilter) = copy(entries - filter)

  }






