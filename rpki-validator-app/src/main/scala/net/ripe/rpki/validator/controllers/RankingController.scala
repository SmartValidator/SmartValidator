package net.ripe.rpki.validator.controllers

import grizzled.slf4j.Logging
import net.ripe.rpki.validator.bgp.preview.BgpValidatedAnnouncement
import net.ripe.rpki.validator.models.{AsRankings, RtrPrefix, ValidatedObjects}
import net.ripe.rpki.validator.ranking.RankingSet
import net.ripe.rpki.validator.views
import net.ripe.rpki.validator.views.RankingView

/**
  * Created by fimka on 05/11/16.
  */
trait RankingController extends ApplicationController with Logging {
  protected def asRankings: AsRankings
  protected def validatedObjects: ValidatedObjects
  private def baseUrl = views.Tabs.AsRankingTab.url
  private def getCurrentRtrPrefixes(): Iterable[RtrPrefix] = validatedObjects.getValidatedRtrPrefixes
  protected def aSrankingSets: Seq[RankingSet]
  protected def validatedAnnouncements: Seq[BgpValidatedAnnouncement]

  get(baseUrl) {
    new RankingView(asRankings, aSrankingSets, getCurrentRtrPrefixes,validatedAnnouncements, messages = feedbackMessages)
  }

//  get(baseUrl) {
//
//  }
//
//  post(baseUrl)
//  {
//
//  }
}
