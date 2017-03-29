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

import scala.xml._
import models._
import lib.Validation._
import net.ripe.ipresource.{Asn, IpRange}
import bgp.preview.BgpValidatedAnnouncement

class PathEndPreviewView(pathEndTable: PathEndTable,localPathEndNeighbors: LocalPathEndNeighbors,validatedAnnouncements: Seq[BgpValidatedAnnouncement],myAsn: Asn, params: Map[String, String] = Map.empty, messages: Seq[FeedbackMessage] = Seq.empty) extends View with ViewHelpers {
  private val fieldNameToText = Map("asn" -> "Origin", "prefix" -> "Prefix", "maxPrefixLength" -> "Maximum prefix length")

  def tab = Tabs.PathEndPreviewTab
  def title = Text("Path-End")
  def body = {
    <div>{ renderMessages(messages, fieldNameToText) }</div>
      <div class="alert-message block-message info" data-alert="alert">
        <a class="close" href="#">×</a>
        <p>
          TODO complete
        </p>
        <p>
          TODO complete
        </p>
      </div>
      <h2>Add entry</h2>
      <div class="well">
        <form method="POST" class="form-stacked">
          <fieldset>
            <div>
              <div class="span4"><label for="announcement-asn">Origin</label></div>
              <div class="span4"><label for="announcement-prefix">Prefix</label></div>
              <div class="span4"><label for="announcement-maxprefixlen">Maximum prefix length</label></div>
              <div class="span4"></div>
            </div>
            <div class="span4">
              <input id="announcement-asn" type="text" name="asn" value={ params.getOrElse("asn", "") } placeholder="ASN (required)"/>
            </div>
            <div class="span4">
              <input id="announcement-prefix" type="text" name="prefix" value={ params.getOrElse("prefix", "") } placeholder="IPv4 or IPv6 prefix (required)"/>
            </div>
            <div class="span4">
              <input id="announcement-maxprefixlen" type="text" name="maxPrefixLength" value={ params.getOrElse("maxPrefixLength", "") } placeholder="Number (optional)"/>
            </div>
            <div class="span2">
              <input type="submit" class="btn primary" value="Add"/>
            </div>
          </fieldset>
        </form>
      </div>
      <div>
        <h2>Local PathEnd Neighbors</h2>{
        if (localPathEndNeighbors.entries.isEmpty)
          <div class="alert-message block-message"><p>No Local As Neighbors defined.</p></div>
        else {
          <table id="whitelist-table" class="zebra-striped" style="display: none;">
            <thead>
              <tr>
                <th>Origin Asn</th><th>Trusted Neighbor</th><th>Validates</th><th>Invalidates</th><th>&nbsp;</th>
              </tr>
            </thead>
            <tbody>{
              for (entry <- localPathEndNeighbors.entries) yield {

                val affectedAnnouncements = validatedAnnouncements.filter { announcement =>
                  entry.contains(announcement.asn)
                }

                // Will work because we only match on affected announcements and will have no unknowns
                var (validated, invalidated) = affectedAnnouncements.partition(_.validity == RouteValidity.Valid)

                // Validates only matches on asn
                validated = validated.filter { _.asn == entry }

                def makeDetailsTable(announcements: Seq[BgpValidatedAnnouncement]) = {
                  <table>
                    <thead>
                      <tr><th>ASN</th><th>Prefix</th></tr>
                    </thead>
                    {
                    for { announcement <- announcements } yield {
                      <tr>
                        <td> { announcement.asn.getValue.toString } </td>
                        <td> { announcement.prefix.toString } </td>
                      </tr>
                    }
                    }
                  </table>
                }
                <tr>
                  <td>myAsn.getValue</td>
                  <td>{entry.getValue}</td>
                  <td>
                    <span rel="popover" data-content={ Xhtml.toXhtml(makeDetailsTable(validated)) } data-original-title="Details">{ validated.size + " announcement(s)" }</span>
                  </td>
                  <td>
                    <span rel="popover" data-content={ Xhtml.toXhtml(makeDetailsTable(invalidated)) } data-original-title="Details">{ invalidated.size + " announcement(s)" }</span>
                  </td>
                  <td>
                    <form method="POST" action="/whitelist" style="padding:0;margin:0;">
                      <input type="hidden" name="_method" value="DELETE"/>
                      <input type="hidden" name="myAsn" value={ myAsn.getValue.toString }/>
                      <input type="hidden" name="asn" value={ entry.toString }/>
                      <input type="submit" class="btn" value="Delete"/>
                    </form>
                  </td>
                </tr>
              }
              }</tbody>
          </table>
            <script><!--
$(document).ready(function() {
  $('#whitelist-table').dataTable({
      "sPaginationType": "full_numbers",
      "aoColumns": [
        { "sType": "numeric" },
        null,
        { "sType": "numeric" },
        { "bSortable": false },
        { "bSortable": false }
      ]
    }).show();
  $('[rel=popover]').popover({
    "live": true,
    "html": true,
    "placement": "below",
    "offset": 10
  }).live('click', function (e) {
    e.preventDefault();
  });
});
// --></script>
        }
        }
      </div>
      <div>
        <h2>Current PathEnd entries</h2>{
        if (pathEndTable.entries.isEmpty)
          <div class="alert-message block-message"><p>No Path Records Found.</p></div>
        else {
          <table id="pathend-table" class="zebra-striped" style="display: none;">
            <thead>
              <tr>
                <th>Origin Asn</th><th>Asn Neighbors</th><th>PathEnd Origin Server</th>
              </tr>
            </thead>
            <tbody>{
              for (entry <- pathEndTable.entries) yield {

                val trustedAsns = entry.trustedAsns

                def makeDetailsTable(asnEntries: scala.collection.mutable.Set[Asn]) = {
                  <table>
                    <thead>
                      <tr><th>ASN</th></tr>
                    </thead>
                    {
                    for {asnEntry <- asnEntries } yield {
                      <tr>
                        <td> { asnEntry.getValue.toString } </td>
                      </tr>
                    }
                    }
                  </table>
                }

                <tr>
                  <td>{ entry.originAsn.getValue() }</td>
                  <td>
                    <span rel="popover" data-content={ Xhtml.toXhtml(makeDetailsTable(trustedAsns)) } data-original-title="Details">{ trustedAsns.size + " Trusted Asns(s)" }</span>
                  </td>
                  <td>BGPSafe PathEnd server</td>
                </tr>
              }
              }</tbody>
          </table>
            <script><!--
$(document).ready(function() {
  $('#pathend-table').dataTable({
      "sPaginationType": "full_numbers",
      "aoColumns": [
        { "sType": "numeric" },
        { "bSortable": false },
        { "sType": "numeric" }

      ]
    }).show();
  $('[rel=popover]').popover({
    "live": true,
    "html": true,
    "placement": "below",
    "offset": 10
  }).live('click', function (e) {
    e.preventDefault();
  });
});
// --></script>
        }
        }
      </div>
  }
}
