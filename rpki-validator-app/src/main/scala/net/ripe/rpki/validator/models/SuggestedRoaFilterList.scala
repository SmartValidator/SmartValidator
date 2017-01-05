package net.ripe.rpki.validator
package models


import net.ripe.ipresource.{Asn, IpRange}

/**
  * Created by fimka on 08/12/16.
  */
  case class SuggestedRoaFilter(asn: Asn, prefix: IpRange, maxLength: Integer, var block: Boolean = false, var fix: Boolean = false) {

  }

  case class SuggestedRoaFilterList(entries: Set[SuggestedRoaFilter] = Set.empty) {

  }






