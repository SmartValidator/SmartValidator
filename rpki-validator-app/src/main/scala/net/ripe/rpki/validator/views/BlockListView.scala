package net.ripe.rpki.validator.views

import net.ripe.rpki.validator.lib.Validation.FeedbackMessage
import net.ripe.rpki.validator.models.{BlockList, RtrPrefix}

/**
  * Created by fimka on 14/10/16.
  */
class BlockListView(blockList: BlockList, getCurrentRtrPrefixes: () => Iterable[RtrPrefix], params: Map[String, String] = Map.empty, messages: Seq[FeedbackMessage] = Seq.empty) extends View with ViewHelpers {
  private val fieldNameToText = Map("prefix".->("Prefix"))
  val currentRtrPrefixes = getCurrentRtrPrefixes()

  def tab = Tabs.FiltersTab
  def title = tab.text
  def body =
  {
    <div></div>
  }
}
