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
 // val papo: String = "papopapo"
  def body = {
    <div class="ct-chart ct-perfect-fourth">
      {val papo: String = "papopapo"}
      <script type="text/javascript" charset="utf-8"><!--
      var lala = '<%=papo%>';
        var data = {
    // A labels array that can contain any sort of values
    labels: [lala,'Tue', 'Wed', 'Thu', 'Fri'],
    // Our series array that contains series objects or in this case series data arrays
    series: [
      [5, 2, 4, 2, 0]
    ]
  };

  // Create a new line chart object where as first parameter we pass in a selector
  // that is resolving to our chart container element. The Second parameter
  // is the actual data object.
  new Chartist.Line('.ct-chart', data);
    //--></script>

    </div>

  }
}
