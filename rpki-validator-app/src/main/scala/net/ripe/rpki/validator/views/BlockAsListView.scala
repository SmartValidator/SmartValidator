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
    <div id="validatedroasdiv">
      <h1 id = "ConflictRoasHead" style ="font-weight: bold;font-size: 21px;">Conflicted ROAs</h1>
      <h2> </h2>
    </div>
    <div id="validatedRoasVSfilters"></div>
    <div id="bgpAnnouncementsheaddiv">
      <h1 id = "BGPannHead" style ="font-weight: bold;font-size: 21px;">BGP's Annoucments Status</h1>
      <h4> </h4>
    </div>
    <div id="bgpAnnouncements"></div>
    <div id="roaIssues">
      <h1 id = "RoaIssueHead" style ="font-weight: bold;font-size: 21px;">Roa Issues Status</h1>
      <h4> </h4></div>
    <div id="roaPerTrustAnchor"></div>
    <div class="ct-chart"></div>
    <div class="ct-chart-pie"></div>

      <script src="/javascript/chartist/access-plugin/chartist-plugin-accessibility.js"></script>
      <script type="text/javascript" charset="utf-8"><!--
             $.getJSON( "/roaIssues.json", function(roaIssues) {
        console.log( "Working..." );
      })
        .done(function(roaIssues) {
          console.log( "second success" );
          console.log( roaIssues );
          new Chartist.Bar('#roaIssues',roaIssues, {
            distributeSeries: true
          });


        });



              $.getJSON( "/bgpAnnouncements.json", function(bgpAnnouncements) {
        console.log( "Working..." );
      })
        .done(function(bgpAnnouncements) {
          console.log( "second success" );
          console.log( bgpAnnouncements );
          var chart = new Chartist.Pie('#bgpAnnouncements', bgpAnnouncements, {
            showLabel: false,
            dount: true,
            plugins: [
                       Chartist.plugins.legend({
            legendNames: ['Valid ' + bgpAnnouncements.series[0].toString(), 'Invalid ' + bgpAnnouncements.series[1].toString(), 'Unknown ' + bgpAnnouncements.series[2].toString()]
        })
               ]
          });
            chart.on('draw', function(data) {
              if(data.type === 'slice') {
                // Get the total path length in order to use for dash array animation
                var pathLength = data.element._node.getTotalLength();

                // Set a dasharray that matches the path length as prerequisite to animate dashoffset
                data.element.attr({
                  'stroke-dasharray': pathLength + 'px ' + pathLength + 'px'
                });

                // Create animation definition while also assigning an ID to the animation for later sync usage
                var animationDefinition = {
                  'stroke-dashoffset': {
                    id: 'anim' + data.index,
                    dur: 1000,
                    from: -pathLength + 'px',
                    to:  '0px',
                    easing: Chartist.Svg.Easing.easeOutQuint,
                    // We need to use `fill: 'freeze'` otherwise our animation will fall back to initial (not visible)
                    fill: 'freeze'
                  }
                };

                // If this was not the first slice, we need to time the animation so that it uses the end sync event of the previous animation
                if(data.index !== 0) {
                  animationDefinition['stroke-dashoffset'].begin = 'anim' + (data.index - 1) + '.end';
                }

                // We need to set an initial value before the animation starts as we are not in guided mode which would do that for us
                data.element.attr({
                  'stroke-dashoffset': -pathLength + 'px'
                });

                // We can't use guided mode as the animations need to rely on setting begin manually
                // See http://gionkunz.github.io/chartist-js/api-documentation.html#chartistsvg-function-animate
                data.element.animate(animationDefinition, false);
              }
            });

            // For the sake of the example we update the chart every time it's created with a delay of 8 seconds
            chart.on('created', function() {
              if(window.__anim21278907124) {
                clearTimeout(window.__anim21278907124);
                window.__anim21278907124 = null;
              }
              window.__anim21278907124 = setTimeout(chart.update.bind(chart), 500000);
            });

        });


          $.getJSON( "/validatedRoasVSfilters.json", function(validatedRoasVSfilters) {
          console.log( "Working..." );
        })
          .done(function(validatedRoasVSfilters) {
            console.log( "second success" );
            console.log( validatedRoasVSfilters );
            var data = validatedRoasVSfilters
            var percent = ((data.series[1] / data.series[0])*100).toString()
            percent = Math.round(percent*100)/100;
            document.getElementById('ConflictRoasHead').innerHTML = "Conflicted ROAs - Currently  " + percent + "% ROAs are in conflict"

            var chart = new Chartist.Pie('#validatedRoasVSfilters', data, {
              donut: true,
              showLabel: false,
              plugins: [
                   Chartist.plugins.legend({
            legendNames: ['Total number of ROAs ' + validatedRoasVSfilters.series[0].toString(), 'Filtered ROAs ' + validatedRoasVSfilters.series[1].toString()]
            })
              ]

            });

            chart.on('draw', function(data) {
              if(data.type === 'slice') {
                // Get the total path length in order to use for dash array animation
                var pathLength = data.element._node.getTotalLength();

                // Set a dasharray that matches the path length as prerequisite to animate dashoffset
                data.element.attr({
                  'stroke-dasharray': pathLength + 'px ' + pathLength + 'px'
                });

                // Create animation definition while also assigning an ID to the animation for later sync usage
                var animationDefinition = {
                  'stroke-dashoffset': {
                    id: 'anim' + data.index,
                    dur: 1000,
                    from: -pathLength + 'px',
                    to:  '0px',
                    easing: Chartist.Svg.Easing.easeOutQuint,
                    // We need to use `fill: 'freeze'` otherwise our animation will fall back to initial (not visible)
                    fill: 'freeze'
                  }
                };

                // If this was not the first slice, we need to time the animation so that it uses the end sync event of the previous animation
                if(data.index !== 0) {
                  animationDefinition['stroke-dashoffset'].begin = 'anim' + (data.index - 1) + '.end';
                }

                // We need to set an initial value before the animation starts as we are not in guided mode which would do that for us
                data.element.attr({
                  'stroke-dashoffset': -pathLength + 'px'
                });

                // We can't use guided mode as the animations need to rely on setting begin manually
                // See http://gionkunz.github.io/chartist-js/api-documentation.html#chartistsvg-function-animate
                data.element.animate(animationDefinition, false);
              }
            });

            // For the sake of the example we update the chart every time it's created with a delay of 8 seconds
            chart.on('created', function() {
              if(window.__anim21278907124) {
                clearTimeout(window.__anim21278907124);
                window.__anim21278907124 = null;
              }
              window.__anim21278907124 = setTimeout(chart.update.bind(chart), 500000);
            });
          });



    //--></script>


  }
}
