package net.ripe.rpki.validator.views

import net.ripe.rpki.validator.lib.Validation.FeedbackMessage
import net.ripe.rpki.validator.models.{BlockAsFilter, BlockAsList}

import scala.xml.Text

/**
  * Created by fimka on 14/10/16.
  */
class BlockAsListView(blockAsList: BlockAsList, params: Map[String, String] = Map.
  empty, messages: Seq[FeedbackMessage] = Seq.empty) extends View with ViewHelpers {
  private val fieldNameToText = Map("asn" -> "Asn")
  //  val currentRtrPrefixes = getCurrentRtrPrefixes()

  def tab = Tabs.BlockAsListTab
  def title = Text("BlockAslist")
  def body = {
    <div>{ renderMessages(messages, fieldNameToText) }</div>
      <div class="alert-message block-message info" data-alert="alert">
        <a class="close" href="#">×</a>
      </div>
      <div>
      </div>
      <h2>Add entry</h2>
      <div class="well">
        <form method="POST" class="form-stacked">
          <fieldset>
            <div>
              <div class="span4"><label for="block-asn">Asn</label></div>
              <div class="span12"></div>
            </div>
            <div class="span4">
              <input id="block-asn" type="text" name="asn" value={ params.getOrElse("asn", "") }
                     placeholder="Asn number"/>
              <input id="block-asn" type="hidden" name="origin" value={ "Manual" }
              />
            </div>
            <div class="span2">
              <input type="submit" class="btn primary" value="Add"/>
            </div>
          </fieldset>
        </form>
      </div>
      <div>
        <h2>Current entries</h2>{
        <table id="blockaslist-table" class="zebra-striped" style="display: none;">
          <thead>
            <tr>
              <th>Asn</th><th>Origin</th><th>&nbsp;</th>
            </tr>
          </thead>
          <tbody>{
            val createTable: Set[BlockAsFilter] =  (blockAsList.entries)
            for (entry <- createTable) yield {
              <tr>
                <td>{ entry.asn}</td>
                <td>{entry.origin}</td>
                <td>
                  <form method="POST" action="/blockAsList" style="padding:0;margin:0;">
                    <input type="hidden" name="_method" value="DELETE"/>
                    <input type="hidden" name="asn" value={ entry.asn.toString }/>
                    <input type="hidden" name="origin" value={ entry.origin.toString}/>
                    <input type="submit" class="btn" value="Unblock"/>
                  </form>
                </td>
              </tr>
            }
            } </tbody>
        </table>
          <script><!--
$(document).ready(function() {
  $('#blockaslist-table').dataTable({
      "sPaginationType": "full_numbers",
      "aoColumns": [null,
        null,
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
