package net.ripe.rpki.validator.views

import net.ripe.rpki.validator.lib.Validation.FeedbackMessage
import net.ripe.rpki.validator.models.{BlockAsFilter, LooseRoa, SuggestedRoaFilterList,SuggestedRoaFilter}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by fimka on 08/12/16.
  */
class RoaBlackListView (suggestedRoaFilterList: SuggestedRoaFilterList, looseRoas:ArrayBuffer[LooseRoa], params: Map[String, String] = Map.empty,
                        messages: Seq[FeedbackMessage] = Seq.empty) extends View with ViewHelpers {
    private val fieldNameToText = Map("prefix" -> "Prefix")

    def tab = Tabs.RoaBlackList
    def title = tab.text
    def body = {
//      <div>
//        { renderMessages(messages, fieldNameToText) }
//        <table>
//          <thead>
//            <tr><th>ASN</th><th>Prefix</th><th>Maxlength</th></tr>
//          </thead>
//          {
//          if(looseRoas.isEmpty)
//          {
//            <h4> No VALID ROA's were found for this IP </h4>
//          }
//          else
//          {
//            for ( releventRoa <- looseRoas ) yield {
//              <tr>
//                <td> { releventRoa.asn.toString } </td>
//                <td> { releventRoa.prefix.toString } </td>
//                <td> { releventRoa.maxLength.toString } </td>
//
//              </tr>
//            }
//          }
//
//          }
//        </table>
//      </div>

        <div>{renderMessages(messages, fieldNameToText) }</div>
        <div class="alert-message block-message info" data-alert="alert">
          <a class="close" href="#">×</a>
        </div>
        <div>
          <h2>Current entries</h2>{
          <table id="blockaslist-table" class="zebra-striped" style="display: none;">
            <thead>
              <tr>
                <th>ASN</th><th>Prefix</th><th>Maxlength</th><th>&nbsp;</th><th>&nbsp;</th>
              </tr>
            </thead>
            <tbody>{
              val createTable: scala.collection.mutable.Set[SuggestedRoaFilter] =  (suggestedRoaFilterList.entries)
              for (entry <- createTable) yield {
                <tr>
                  <td>{entry.asn}</td>
                  <td>{entry.prefix}</td>
                  <td>{entry.maxLength}</td>
                  <td>
                    <form method="POST" action="/blockAsList" style="padding:0;margin:0;">
                      <input type="hidden" name="_method" value="POST"/>
                      <input type="hidden" name="asn" value={ entry.asn.toString }/>
                      <input type="hidden" name="prefix" value={entry.prefix.toString}/>
                      <input type="hidden" name="maxlength" value={entry.maxLength.toString}/>
                      <input type="hidden" name="block" value={entry.block.toString}/>
                      <input type="hidden" name="fix" value={entry.fix.toString}/>
                      <input type="submit" class="btn" value="Block"/>
                    </form>
                  </td>
                  <td>
                    <form method="POST" action="/blockAsList" style="padding:0;margin:0;">
                      <input type="hidden" name="_method" value="DELETE"/>
                      <input type="hidden" name="asn" value={ entry.asn.toString }/>
                      <input type="hidden" name="prefix" value={entry.prefix.toString}/>
                      <input type="hidden" name="maxlength" value={entry.maxLength.toString}/>
                      <input type="hidden" name="block" value={entry.block.toString}/>
                      <input type="hidden" name="fix" value={entry.fix.toString}/>
                      <input type="submit" class="btn" value="Fix"/>
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
