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
