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
package net.ripe.rpki.validator
package views

import net.ripe.rpki.validator.lib.{NewVersionDetails, UserPreferences}

import scala.xml._

object Layouts {
  def none(view: View): NodeSeq = {
    <html lang="en">
      <head>
        <meta charset="utf-8"/>
      </head>
      <body>{ view.body }</body>
    </html>
  }

  def standard(view: View, newVersionDetails: Option[NewVersionDetails], userPreferences: UserPreferences): NodeSeq = {
    <html lang="en">
      <head>
        <meta charset="utf-8"/>
        <title>SaferHop - { view.title }</title>
        <link rel="stylesheet" href="/stylesheets/bootstrap/1.3.0/bootstrap.css"/>
        <link rel="stylesheet" href="/stylesheets/application.css"/>
        <script src="/javascript/datatables/1.8.2/jquery.js"/>
        <script src="/javascript/datatables/1.8.2/jquery.dataTables.min.js"/>
        <script src="/javascript/bootstrap/1.3.0/bootstrap-alerts.js"/>
        <script src="/javascript/bootstrap/1.3.0/bootstrap-twipsy.js"/>
        <script src="/javascript/bootstrap/1.3.0/bootstrap-popover.js"/>
        <link rel="stylesheet" href="/javascript/chartist/chartist.min.css"></link>
        <script src="/javascript/chartist/chartist.min.js"></script>
        <script src="/javascript/chartist/legend/chartist-plugin-legend.js"></script>
        <link rel="stylesheet" href="/javascript/chartist/legend/legendcss.css"></link>
        <script src="/javascript/bootstrap/1.4.0/bootstrap-dropdown.js"/>
        <script type="text/javascript" charset="utf-8"><!--
            $(document).ready(function(){
                $("#feedbackButton").hover(function(){
                        $(this).css("left","0px");
                }, function(){
                        $(this).css("left","-5px");
                });
            $(".dropdown").dropdown();
             });
        // --></script>
      </head>
      <body>
        <div class="topbar">
          <div class="fill">
            <div class="container">
              <a class="brand" href="/">SaferHop</a>
              <ul class="nav">
                {
                  for (tab <- Tabs.visibleTabs) yield {
                    <li class={ if (tab == view.tab) "active" else "" }><a href={ tab.url }>{ tab.text }</a></li>
                  }
                }
              </ul>
            </div>
          </div>
        </div>
        <div class="container">
          {
            val newVersionNotify = newVersionDetails match {
              case Some(versionDetails) => {
                  <p>New version { versionDetails.version } available <a href={ versionDetails.url.toString }>here</a>.</p>
              }
              case None => NodeSeq.Empty
            }

            newVersionNotify match {
              case NodeSeq.Empty => NodeSeq.Empty
              case messages =>
              <div class="alert-message block-message"  data-alert="alert">
                  <a class="close" href="#">×</a>
                  { messages }
              </div>
            }

          }
          <div class="page-header">
            <h1>{ view.title }</h1>
          </div>
          { view.body }

        </div>
        <div id="feedbackButton">
          <a href="mailto:certification@ripe.net?subject=RPKI Validator Feedback"><img src="/images/feedback.png" width="41" height="111" alt="Feedback"/></a>
        </div>
      </body>
    </html>
  }
}
