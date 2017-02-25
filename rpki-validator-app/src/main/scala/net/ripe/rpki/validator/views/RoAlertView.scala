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
package net.ripe.rpki.validator.views

import java.net.UnknownHostException

import net.ripe.ipresource.IpAddress
import net.ripe.rpki.validator.models.{RtrPrefix, ValidatedObjects}

class RoAlertView(validatedObjects: ValidatedObjects) extends View with ViewHelpers {
  val allRecords = validatedObjects.getValidatedRtrPrefixes.toIndexedSeq

  def tab = Tabs.RoAlertTab
  def title = tab.text
  def body = {
    val externalIp = getHostIp
    val filteredRoas = filterRelativeIp(externalIp)
    <div>
      Your IpAddress is: { externalIp.toString() }
      Relevent roa:
      <table>
        <thead>
          <tr><th>ASN</th><th>Prefix</th><th>Max prefix</th></tr>
        </thead>
        {
        if(filteredRoas.isEmpty)
        {
          <h4> No VALID ROA's were found for this IP </h4>
        }
        else
        {
          for { releventRoa <- filterRelativeIp(externalIp) } yield {
            <tr>
              <td> { releventRoa.asn.toString } </td>
              <td> { releventRoa.prefix.toString } </td>
              <td> { releventRoa.maxPrefixLength.toString } </td>
            </tr>
          }
        }

        }
      </table>
    </div>
  }


  private def filterRelativeIp(ipAddress: IpAddress): IndexedSeq[RtrPrefix] = {
    allRecords.filter(_.prefix.contains(ipAddress))
  }



  private def getHostIp: IpAddress = {
    try {
      val addr = scala.io.Source.fromURL("https://api.ipify.org").mkString
      IpAddress.parse(addr)
    }   catch {
      case e: Exception =>
      val unknownHostException = new UnknownHostException("Failed to determine IP address: " + e)
      unknownHostException.initCause(e)
      throw unknownHostException
    }



  }



}
