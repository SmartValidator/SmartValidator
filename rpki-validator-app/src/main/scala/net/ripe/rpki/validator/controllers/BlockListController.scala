package net.ripe.rpki.validator
package controllers

import net.ripe.rpki.validator.bgp.preview.BgpValidatedAnnouncement
import net.ripe.rpki.validator.models.{BlockList, RtrPrefix}
import net.ripe.rpki.validator.views.BlockListView

trait BlockListController extends ApplicationController {
  protected def blockList: BlockList

  protected def addBlockListEntry(entry: RtrPrefix): Unit

  protected def removeBlockListEntry(entry: RtrPrefix): Unit

  protected def blockListEntryExists(entry: RtrPrefix): Boolean = blockList.entries.contains(entry)

  protected def validatedAnnouncements: Seq[BgpValidatedAnnouncement]

  private def baseUrl = views.Tabs.BlockListTab.url


  get(baseUrl) {
    new BlockListView(blockList, validatedAnnouncements, messages = feedbackMessages)
  }

}

