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

import net.ripe.rpki.validator.lib.Validation.FeedbackMessage

import scala.xml.Text


/**
  * Created by fimka on 14/10/16.
  */
class BlockAsListView(params: Map[String, String] = Map.empty, messages: Seq[FeedbackMessage] = Seq.empty) extends View with ViewHelpers {
  private val fieldNameToText = Map("asn" -> "Asn")
  //  val currentRtrPrefixes = getCurrentRtrPrefixes()

  def tab = Tabs.BlockAsListTab
  def title = Text("BlockAslist")
  def body = {
    <div id="validatedRoasVSfilters">    </div>
      <div id="bgpAnnouncements">    </div>
      <div id="roaBgpCollisionsByType">    </div>
      <div id="roaPerTrustAnchor">    </div>

      <script src="/javascript/chartist/access-plugin/chartist-plugin-accessibility.js"></script>

      <script type="text/javascript" charset="utf-8"><!--
          $.getJSON( "/validatedRoasVSfilters.json", function(validatedRoasVSfilters) {
          console.log( "Working..." );
        })
          .done(function(validatedRoasVSfilters) {
            console.log( "second success" );
            console.log( validatedRoasVSfilters );
            new Chartist.Pie('#validatedRoasVSfilters', validatedRoasVSfilters, {}
            )
          });
          $.getJSON( "/bgpAnnouncements.json", function(validatedRoasVSfilters) {
          console.log( "Working..." );
        })
          .done(function(validatedRoasVSfilters) {
            console.log( "second success" );
            console.log( validatedRoasVSfilters );
            new Chartist.Pie('#bgpAnnouncements', validatedRoasVSfilters, {}
            )
          });
    //--></script>


  }
}
