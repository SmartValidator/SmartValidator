package net.ripe.rpki.validator.controllers

import grizzled.slf4j.Logging
import net.ripe.rpki.validator.bgp.preview.BgpValidatedAnnouncement
import net.ripe.rpki.validator.models.{RoaBlackList, RtrPrefix, ValidatedObjects}
import net.ripe.rpki.validator.views
import net.ripe.rpki.validator.views.RoaBlackListView

/**
  * Created by fimka on 08/12/16.
  */
trait RoaBlackListController extends ApplicationController with Logging {
  protected def roaBlackList: RoaBlackList
  private def baseUrl = views.Tabs.RoaBlackList.url
  protected def validatedObjects: ValidatedObjects
  private def getCurrentRtrPrefixes(): Iterable[RtrPrefix] = validatedObjects.getValidatedRtrPrefixes
  protected def validatedAnnouncements: IndexedSeq[BgpValidatedAnnouncement]


  get(baseUrl){
    new RoaBlackListView(roaBlackList, getCurrentRtrPrefixes, validatedAnnouncements, messages = feedbackMessages)
  }


}
