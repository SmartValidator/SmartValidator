package net.ripe.rpki.validator.models

import net.ripe.ipresource.IpRange

/**
  * Created by fimka on 05/11/16.
  */
  case class AsRankingBlocker(prefix: IpRange) {
    def shouldBlock(rtrPrefix: RtrPrefix): Boolean = prefix.overlaps(rtrPrefix.prefix)
  }

  case class AsRankings(entries: Set[AsRankingBlocker] = Set.empty) {
    def addAsRank(filter: AsRankingBlocker) = copy(entries + filter)
    def removeAsRank(filter: AsRankingBlocker) = copy(entries - filter)

    def filter(input: Iterable[RtrPrefix]): Iterable[RtrPrefix] =
      if (entries.isEmpty) input
      else input.filterNot(shouldBlock)

    private def shouldBlock(rtrPrefix: RtrPrefix) = entries.exists(_.shouldBlock(rtrPrefix))
  }

