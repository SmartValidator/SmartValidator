package net.ripe.rpki.validator.models

import net.ripe.ipresource.{Asn, IpRange}

/**
  * Created by fimka on 15/12/16.
  */

  case class LooseRoa(asn: Asn, prefix: IpRange, maxLength: Int){

  }

  object LooseRoas{}
    //    protected def validatedAnnouncements: IndexedSeq[BgpValidatedAnnouncement]
    //
    //    private def isRoaLoose(roa: RtrPrefix): Boolean = {
    //      var maxAdvertisiedLength:Int = 0
    //      var roaMaxLength : Int = roa.prefix.getPrefixLength
    //      if(roa.maxPrefixLength.isDefined){
    //        roaMaxLength = roa.maxPrefixLength.get
    //      }
    //
    //      for(bgpAnnouncement <- validatedAnnouncements){
    //        if(roa.prefix.contains(bgpAnnouncement.prefix)) // change to think on maxLength too
    //        {
    //          if(maxAdvertisiedLength < bgpAnnouncement.prefix.getPrefixLength){
    //            maxAdvertisiedLength = bgpAnnouncement.prefix.getPrefixLength
    //          }
    //
    //          if(maxAdvertisiedLength == roaMaxLength){
    //            return true
    //          }
    //        }
    //      }
    //
    //      false
    //    }
    //
    //    private def findLooseROA() : ArrayBuffer[LooseRoa] = {
    //      var looseRoas = ArrayBuffer[LooseRoa]()
    //      val currentRoas = getCurrentRtrPrefixes()
    //      currentRoas.foreach{ roa =>
    //        if(isRoaLoose(roa)){
    //          val temp = LooseRoa(roa.asn, roa.prefix, roa.maxPrefixLength.getOrElse(roa.prefix.getPrefixLength))
    //          looseRoas += temp
    //        }
    //      }
    //      looseRoas
    //    }

