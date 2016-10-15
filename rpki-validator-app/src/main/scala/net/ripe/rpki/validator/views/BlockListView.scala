package net.ripe.rpki.validator.views

import net.ripe.rpki.validator.bgp.preview.BgpValidatedAnnouncement
import net.ripe.rpki.validator.lib.Validation.FeedbackMessage
import net.ripe.rpki.validator.models.BlockList

import scala.xml.Text

/**
  * Created by fimka on 14/10/16.
  */
class BlockListView(blockList: BlockList, validatedAnnouncements: Seq[BgpValidatedAnnouncement], params: Map[String, String] = Map.empty, messages: Seq[FeedbackMessage] = Seq.empty) extends View with ViewHelpers {
  private val fieldNameToText = Map("asn" -> "Origin", "prefix" -> "Prefix", "maxPrefixLength" -> "Maximum prefix length")
//  val currentRtrPrefixes = getCurrentRtrPrefixes()

  def tab = Tabs.BlockListTab
  def title = Text("Whitelist")
  def body =
  {
    <div></div>
  }
}
