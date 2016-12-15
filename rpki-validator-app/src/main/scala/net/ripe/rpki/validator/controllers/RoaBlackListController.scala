package net.ripe.rpki.validator.controllers

import grizzled.slf4j.Logging
import net.ripe.rpki.validator.bgp.preview.BgpValidatedAnnouncement
import net.ripe.rpki.validator.models.{LooseRoa, RoaBlackList, RtrPrefix, ValidatedObjects}
import net.ripe.rpki.validator.views
import net.ripe.rpki.validator.views.RoaBlackListView

import scala.collection.mutable.ArrayBuffer

/**
  * Created by fimka on 08/12/16.
  */
trait RoaBlackListController extends ApplicationController with Logging {
  protected def roaBlackList: RoaBlackList
  private def baseUrl = views.Tabs.RoaBlackList.url
  protected def validatedObjects: ValidatedObjects
  private def getCurrentRtrPrefixes(): Iterable[RtrPrefix] = validatedObjects.getValidatedRtrPrefixes
  protected def validatedAnnouncements: IndexedSeq[BgpValidatedAnnouncement]
  protected var looseRoas = ArrayBuffer[LooseRoa]()


  private def isRoaLoose(roa: RtrPrefix): Boolean = {
    var maxAdvertisiedLength:Int = 0
    var roaMaxLength : Int = roa.prefix.getPrefixLength
    if(roa.maxPrefixLength.isEmpty){
      roaMaxLength = roa.prefix.getPrefixLength
    }

    for(bgpAnnouncement <- validatedAnnouncements){
      if(roa.prefix.contains(bgpAnnouncement.prefix))
      {
        if(maxAdvertisiedLength < bgpAnnouncement.prefix.getPrefixLength){
          maxAdvertisiedLength = bgpAnnouncement.prefix.getPrefixLength
        }

        if(maxAdvertisiedLength == roaMaxLength){
          return true
        }
      }
    }

    false
  }

  private def findLooseROA() {
    val currentRoas = getCurrentRtrPrefixes()
    currentRoas.foreach{ roa =>
          if(isRoaLoose(roa)){
            val temp = LooseRoa(roa.asn, roa.prefix)
            looseRoas += temp
          }
        }
  }

  get(baseUrl){
    new RoaBlackListView(roaBlackList, looseRoas, messages = feedbackMessages)
  }


}
