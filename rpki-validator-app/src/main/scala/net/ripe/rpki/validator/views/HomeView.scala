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
class HomeView(params: Map[String, String] = Map.empty, messages: Seq[FeedbackMessage] = Seq.empty) extends View with ViewHelpers {
  private val fieldNameToText = Map("asn" -> "Asn")
  //  val currentRtrPrefixes = getCurrentRtrPrefixes()

  def tab = Tabs.HomeTab
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
    <div id="conflitsTimeline">
      <h1 id = "conflitsTimelineHead" style ="font-weight: bold;font-size: 21px;">Roa Issues Status</h1>
      <h4> </h4></div>
    <div class="ct-chart"></div>
    <div class="ct-chart-pie"></div>

      <script src="/javascript/chartist/access-plugin/chartist-plugin-accessibility.js"></script>
      <script type="text/javascript" charset="utf-8"><!--

        $.getJSON( "/conflitsTimeline.json", function(conflitsTimeline) {
        console.log( "Working..." );
      })
        .done(function(conflitsTimeline) {
          console.log( "second success" );
          console.log( conflitsTimeline );
          var conflitsTimelineChart = new Chartist.Line('#conflitsTimeline',conflitsTimeline, {
            low: 0
          });

       // Let's put a sequence number aside so we can use it in the event callbacks
        var seq = 0,
          delays = 80,
          durations = 500;

        // Once the chart is fully created we reset the sequence
        conflitsTimelineChart.on('created', function() {
          seq = 0;
        });

        // On each drawn element by Chartist we use the Chartist.Svg API to trigger SMIL animations
        conflitsTimelineChart.on('draw', function(data) {
          seq++;

          if(data.type === 'line') {
            // If the drawn element is a line we do a simple opacity fade in. This could also be achieved using CSS3 animations.
            data.element.animate({
              opacity: {
                // The delay when we like to start the animation
                begin: seq * delays + 1000,
                // Duration of the animation
                dur: durations,
                // The value where the animation should start
                from: 0,
                // The value where it should end
                to: 1
              }
            });
          } else if(data.type === 'label' && data.axis === 'x') {
            data.element.animate({
              y: {
                begin: seq * delays,
                dur: durations,
                from: data.y + 100,
                to: data.y,
                // We can specify an easing function from Chartist.Svg.Easing
                easing: 'easeOutQuart'
              }
            });
          } else if(data.type === 'label' && data.axis === 'y') {
            data.element.animate({
              x: {
                begin: seq * delays,
                dur: durations,
                from: data.x - 100,
                to: data.x,
                easing: 'easeOutQuart'
              }
            });
          } else if(data.type === 'point') {
            data.element.animate({
              x1: {
                begin: seq * delays,
                dur: durations,
                from: data.x - 10,
                to: data.x,
                easing: 'easeOutQuart'
              },
              x2: {
                begin: seq * delays,
                dur: durations,
                from: data.x - 10,
                to: data.x,
                easing: 'easeOutQuart'
              },
              opacity: {
                begin: seq * delays,
                dur: durations,
                from: 0,
                to: 1,
                easing: 'easeOutQuart'
              }
            });
          } else if(data.type === 'grid') {
            // Using data.axis we get x or y which we can use to construct our animation definition objects
            var pos1Animation = {
              begin: seq * delays,
              dur: durations,
              from: data[data.axis.units.pos + '1'] - 30,
              to: data[data.axis.units.pos + '1'],
              easing: 'easeOutQuart'
            };

            var pos2Animation = {
              begin: seq * delays,
              dur: durations,
              from: data[data.axis.units.pos + '2'] - 100,
              to: data[data.axis.units.pos + '2'],
              easing: 'easeOutQuart'
            };

            var animations = {};
            animations[data.axis.units.pos + '1'] = pos1Animation;
            animations[data.axis.units.pos + '2'] = pos2Animation;
            animations['opacity'] = {
              begin: seq * delays,
              dur: durations,
              from: 0,
              to: 1,
              easing: 'easeOutQuart'
            };

            data.element.animate(animations);
          }
        });

        // For the sake of the example we update the chart every time it's created with a delay of 10 seconds
        conflitsTimelineChart.on('created', function() {
          if(window.__exampleAnimateTimeout) {
            clearTimeout(window.__exampleAnimateTimeout);
            window.__exampleAnimateTimeout = null;
          }
          window.__exampleAnimateTimeout = setTimeout(conflitsTimelineChart.update.bind(conflitsTimelineChart), 12000);
        });

        });
             $.getJSON( "/roaIssues.json", function(roaIssues) {
        console.log( "Working..." );
      })
        .done(function(roaIssues) {
          console.log( "second success" );
          console.log( roaIssues );
           $(".ct-series-a .ct-bar").css('stroke', 'black');
          var roaIssueChart = new Chartist.Bar('#roaIssues',roaIssues, {
            distributeSeries: true,
          // distance between bars
            seriesBarDistance: 10
          });

          roaIssueChart.on('draw', function(data) {
          if(data.type === 'bar') {
            data.element.animate({
              y2: {
                dur: 1000,
                from: data.y1,
                to: data.y2,
                easing: Chartist.Svg.Easing.easeOutQuint
              },
              opacity: {
                dur: 1000,
                from: 0,
                to: 1,
                easing: Chartist.Svg.Easing.easeOutQuint
              }
            });
          }
        });
              roaIssueChart.on('created', function() {
              if(window.__anim21278907124) {
                clearTimeout(window.__anim21278907124);
                window.__anim21278907124 = null;
              }
              window.__anim21278907124 = setTimeout(roaIssueChart.update.bind(roaIssueChart), 10000);
            });


        });



              $.getJSON( "/bgpAnnouncements.json", function(bgpAnnouncements) {
        console.log( "Working..." );
      })
        .done(function(bgpAnnouncements) {
          console.log( "second success" );
          console.log( bgpAnnouncements );
          var chartbgpAnnouncements = new Chartist.Pie('#bgpAnnouncements', bgpAnnouncements, {
            showLabel: false,
            dount: true,
            plugins: [
                       Chartist.plugins.legend({
            legendNames: ['Valid ' + bgpAnnouncements.series[0].toString(), 'Invalid ' + bgpAnnouncements.series[1].toString(), 'Unknown ' + bgpAnnouncements.series[2].toString()]
        })
               ]
          });
            chartbgpAnnouncements.on('draw', function(data) {
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
            chartbgpAnnouncements.on('created', function() {
              if(window.__anim21278907124) {
                clearTimeout(window.__anim21278907124);
                window.__anim21278907124 = null;
              }
              window.__anim21278907124 = setTimeout(chartbgpAnnouncements.update.bind(chartbgpAnnouncements), 10000);
            });

        });


          $.getJSON( "/validatedRoasVSfilters.json", function(validatedRoasVSfilters) {
          console.log( "Working..." );
        })
          .done(function(validatedRoasVSfilters) {
            console.log( "second success" );
            console.log( validatedRoasVSfilters );
            var data = validatedRoasVSfilters
            var percent = ((data.series[2] / data.series[0])*100).toString()
            percent = Math.round(percent*100)/100;
            document.getElementById('ConflictRoasHead').innerHTML = "Conflicted ROAs - Currently  " + percent + "% ROAs are in conflict"

            var chart = new Chartist.Pie('#validatedRoasVSfilters', data, {
              donut: true,
              showLabel: false,
              plugins: [
                   Chartist.plugins.legend({
            legendNames: ['Total number of ROAs ' + validatedRoasVSfilters.series[0].toString(), 'Filtered ROAs ' + validatedRoasVSfilters.series[1].toString(),'ROAs in conflict ' + validatedRoasVSfilters.series[2].toString()]
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
                window.__anim21278907124 = setTimeout(chart.update.bind(chart), 10000);
              });

          });



    //--></script>


  }
}
