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

import net.ripe.rpki.validator.bgp.preview.BgpValidatedAnnouncement
import net.ripe.rpki.validator.lib.Validation.FeedbackMessage
import net.ripe.rpki.validator.models._
import net.ripe.rpki.validator.ranking.RankingSet

import scala.xml.Xhtml

/**
  * Created by fimka on 05/11/16.
  */
class RankingView(asRankings: AsRankings, asRankingSets : Seq[RankingSet],getCurrentRtrPrefixes: () => Iterable[RtrPrefix], validatedAnnouncements: Seq[BgpValidatedAnnouncement],blockAsList: BlockAsList,params: Map[String, String] = Map.empty, messages: Seq[FeedbackMessage] = Seq.empty) extends View with ViewHelpers {
  private val fieldNameToText = Map("prefix" -> "Prefix")

  val currentRtrPrefixes = getCurrentRtrPrefixes()

  def tab = Tabs.AsRankingTab
  def title = tab.text
  def body = {
    <div>{ renderMessages(messages, fieldNameToText) }</div>
      <div class="alert-message block-message info" data-alert="alert">
        <a class="close" href="#">×</a>
        <p>By adding a filter the validator will ignore any RPKI prefixes that overlap with the filter's prefix.</p>
      </div>
      <h2>Add filter</h2>
      <div class="well">
        <form method="POST" class="form-stacked">
          <fieldset>
            <div>
              <div class="span4"><label for="filter-prefix">Prefix</label></div>
              <div class="span12"></div>
            </div>
            <div class="span4">
              <input id="filter-prefix" type="text" name="prefix" value={ params.getOrElse("prefix", "") } placeholder="IPv4 or IPv6 prefix (required)"/>
            </div>
            <div class="span10">
              <input type="submit" class="btn primary" value="Add"/>
            </div>
          </fieldset>
        </form>
      </div>
      <div>
        <h2>Current entries</h2>{
        <table id="asRanking-table" class="zebra-striped" style="display: none;">
          <thead>
            <tr>
              <th>Asn</th><th>Name</th><th>Rank</th><th>Validates</th><th>Invalidates</th><th>&nbsp;</th>
            </tr>
          </thead>
          <tbody>{

            var work_set = asRankingSets(0)
            for (entry <- work_set.entries) yield {
              val affectedAsn = validatedAnnouncements.filter { announcement =>
                entry.asn.contains(announcement.asn)
              }

              // Will work because we only match on affected announcements and will have no unknowns
              var (validated, invalidated) = affectedAsn.partition(_.validity == RouteValidity.Valid)

              // Validates only matches on asn
              validated = validated.filter { _.asn == entry.asn }

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
              if(!blockAsList.entries.contains(new BlockAsFilter(entry.asn,"RankingAsn"))){
                <tr>
                  <td>{entry.asn}</td>
                  <td>{entry.name }</td>
                  <td>{entry.rank}</td>
                  <td>
                    <span rel="popover" data-content={ Xhtml.toXhtml(makeDetailsTable(validated)) } data-original-title="Details">{ validated.size + " announcement(s)" }</span>
                  </td>
                  <td>
                    <span rel="popover" data-content={ Xhtml.toXhtml(makeDetailsTable(invalidated)) } data-original-title="Details">{ invalidated.size + " announcement(s)" }</span>
                  </td>
                  <td>
                    <form method="POST" action="/asRanking" style="padding:0;margin:0;">
                      <input type="hidden" name="_method" value="POST"/>
                      <input type="hidden" name="asn" value={ entry.asn.toString }/>
                      <input type="hidden" name="name" value={ entry.name.toString }/>
                      <input type="hidden" name="rank" value={ entry.rank.toString }/>
                      <input style="background-image: -webkit-gradient(linear, 0 0, 0 100%, from(#ffffff), color-stop(0%, #ffffff), to(#FF6347));background-color:#FF6347; " type="submit" class="btn" value="Block"/>
                    </form>
                  </td>
                </tr>
              }
              else{
                <tr>
                  <td>{entry.asn}</td>
                  <td>{entry.name }</td>
                  <td>{entry.rank}</td>
                  <td>
                    <span rel="popover" data-content={ Xhtml.toXhtml(makeDetailsTable(validated)) } data-original-title="Details">{ validated.size + " announcement(s)" }</span>
                  </td>
                  <td>
                    <span rel="popover" data-content={ Xhtml.toXhtml(makeDetailsTable(invalidated)) } data-original-title="Details">{ invalidated.size + " announcement(s)" }</span>
                  </td>
                  <td>
                    <form method="POST" action="/asRanking" style="padding:0;margin:0;">
                      <input type="hidden" name="_method" value="DELETE"/>
                      <input type="hidden" name="asn" value={ entry.asn.toString }/>
                      <input type="hidden" name="name" value={ entry.name.toString }/>
                      <input type="hidden" name="rank" value={ entry.rank.toString }/>
                      <input type="submit" class="btn" value="Unblock"/>
                    </form>
                  </td>
                </tr>

              }

            }
            } </tbody>
        </table>
          <script><!--
$(document).ready(function() {
  $('#asRanking-table').dataTable({
      "sPaginationType": "full_numbers",
      "aoColumns": [
        null,
        null,
        null,
        { "bSortable": false },
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
      </div>
  }


}
