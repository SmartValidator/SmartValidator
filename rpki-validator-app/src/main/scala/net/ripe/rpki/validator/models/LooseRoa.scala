/**
 * The BSD License
 *
 * Copyright (c) 2010-2012 RIPE NCC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   - Neither the name of the RIPE NCC nor the names of its contributors may be
 *     used to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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

