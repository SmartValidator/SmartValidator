package net.ripe.rpki.validator.models

import net.ripe.ipresource.IpRange

/**
  * Created by fimka on 08/12/16.
  */
  case class RoaBlackListEntry(prefix: IpRange) {
    def shouldBlock(rtrPrefix: RtrPrefix): Boolean = prefix.overlaps(rtrPrefix.prefix)
  }
  case class RoaBlackList(entries: Set[RoaBlackListEntry] = Set.empty) {

  }

