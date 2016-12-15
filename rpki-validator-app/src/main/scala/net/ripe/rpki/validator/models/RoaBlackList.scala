package net.ripe.rpki.validator.models

import net.ripe.ipresource.{Asn, IpRange}

/**
  * Created by fimka on 08/12/16.
  */
  case class BlockedRoa(asn: Asn, prefix: IpRange, maxLength: Integer) {
    def shouldBlock(rtrPrefix: RtrPrefix): Boolean = prefix.overlaps(rtrPrefix.prefix)
  }

  case class RoaBlackList(entries: Set[BlockedRoa] = Set.empty) {

  }






