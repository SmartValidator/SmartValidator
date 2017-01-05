package net.ripe.rpki.validator.RoaBgpIssues

import net.ripe.ipresource.IpRange
import net.ripe.rpki.validator.bgp.preview.BgpAnnouncement
import net.ripe.rpki.validator.models.RouteValidity.RouteValidity
import net.ripe.rpki.validator.models.RtrPrefix
import org.joda.time.DateTime

/**
  * Created by fimka on 04/01/17.
  */
case class RoaBgpIssue(roa: RtrPrefix, bgpAnnouncements :scala.collection.mutable.Set[(RouteValidity, BgpAnnouncement)]) {
}
case class RoaBgpCollisons(lastModified: DateTime, roaBgpIssuesSet: IndexedSeq[RoaBgpIssue])


class RoaBgpIssueSeeker {
  import scala.concurrent.stm._


  private val _roaBgpIssueSet = Ref(RoaBgpCollisons(DateTime.now(), IndexedSeq.empty[RoaBgpIssue]))


  def roaBgpIssuesSet: RoaBgpCollisons = _roaBgpIssueSet.single.get

}


