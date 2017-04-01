/**
  * The BSD License
  *
  * Copyright (c) 2010-2012 RIPE NCC
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *   - Redistributions of source code must retain the above copyright notice,
  * this list of conditions and the following disclaimer.
  *   - Redistributions in binary form must reproduce the above copyright notice,
  * this list of conditions and the following disclaimer in the documentation
  * and/or other materials provided with the distribution.
  *   - Neither the name of the RIPE NCC nor the names of its contributors may be
  * used to endorse or promote products derived from this software without
  * specific prior written permission.
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
package net.ripe.rpki.validator
package lib


import java.net.URI

import org.joda.time.DateTime
import java.io.InputStream
import java.util.Properties

import grizzled.slf4j.Logging
import org.joda.time.Duration
import java.io.ByteArrayInputStream

import net.ripe.rpki.validator.lib.RoaOperationMode.RoaOperationMode


case class NewVersionDetails(version: String, url: URI)

object RoaOperationMode extends Enumeration {
  type RoaOperationMode = Value
  val AutoModeRemoveBadROA, AutoModeRemoveGoodROA, ManualMode = Value

  def valueOf(name: String) = this.values.find(_.toString == name)

  def isOrderType(s: String) = values.exists(_.toString == s)
}

// Note enableFeedback is an option so that we can see detect if users never made this choice, and prompt them.
case class UserPreferences(updateAlertActive: Boolean = true, maxStaleDays: Int = 10,
                           roaOperationMode: RoaOperationMode = RoaOperationMode.ManualMode, conflictCertDays: Int = 20,
                           roaBgpConflictLearnMode: Boolean = false, maxConflictedBgpStaleDays: Int = 1) {
  require(maxStaleDays >= 0)
  require(maxConflictedBgpStaleDays >= 0)
  require(conflictCertDays >= 0)
}

trait SoftwareUpdateChecker extends Logging {

  private var lastCheck: DateTime = null
  private var cachedNewVersionDetails: Option[NewVersionDetails] = None

  def newVersionDetailFetcher: NewVersionDetailFetcher

  def userPreferences: UserPreferences

  /**
    * Get new version details. Will cache for one day. Returns
    * None in case of problems or when no new version exists.
    */
  def newVersionDetails(): Option[NewVersionDetails] = {
    userPreferences.updateAlertActive match {
      case true => returnNewVersionDetails
      case false => None
    }
  }

  def returnNewVersionDetails = {
    if (lastCheck == null || new Duration(lastCheck, new DateTime()).isLongerThan(Duration.standardDays(1))) {
      cachedNewVersionDetails = newVersionDetailFetcher.readNewVersionDetails
      lastCheck = new DateTime()
    }
    cachedNewVersionDetails
  }
}


trait NewVersionDetailFetcher {
  def readNewVersionDetails: Option[NewVersionDetails]
}


class OnlineNewVersionDetailFetcher(currentVersion: String, getPropertiesString: () => String) extends NewVersionDetailFetcher with Logging {

  override def readNewVersionDetails = {
    try {
      val properties = parseProperties
      val newVersion = properties.getProperty("version.latest")

      if (newVersion != currentVersion) {
        Some(NewVersionDetails(version = newVersion, url = URI.create(properties.getProperty("version.url"))))
      } else {
        None
      }
    } catch {
      case e: Exception =>
        error(e)
        None
    }
  }

  private def parseProperties = {

    var in: InputStream = null
    try {
      val bytes = getPropertiesString().getBytes("UTF-8")
      in = new ByteArrayInputStream(bytes)

      val properties = new Properties()
      properties.load(in)
      properties

    } finally {
      if (in != null) {
        in.close()
      }
    }

  }
}
