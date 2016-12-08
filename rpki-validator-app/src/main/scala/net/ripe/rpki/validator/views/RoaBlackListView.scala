package net.ripe.rpki.validator.views

import net.ripe.rpki.validator.bgp.preview.BgpValidatedAnnouncement
import net.ripe.rpki.validator.lib.Validation.FeedbackMessage
import net.ripe.rpki.validator.models.{RoaBlackList, RtrPrefix}

/**
  * Created by fimka on 08/12/16.
  */
class RoaBlackListView (roaBlackList: RoaBlackList, getCurrentRtrPrefixes: () => Iterable[RtrPrefix],
                        validatedAnnouncements: Seq[BgpValidatedAnnouncement], params: Map[String, String] = Map.empty,
                        messages: Seq[FeedbackMessage] = Seq.empty) extends View with ViewHelpers {
    private val fieldNameToText = Map("prefix" -> "Prefix")
    val currentRtrPrefixes = getCurrentRtrPrefixes()

    def tab = Tabs.RoaBlackList
    def title = tab.text
    def body = {
      <div>
        { renderMessages(messages, fieldNameToText) }
      </div>
    }
}
