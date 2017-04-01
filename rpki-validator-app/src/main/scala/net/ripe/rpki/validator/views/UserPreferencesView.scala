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
package views

import scala.xml._
import lib.{RoaOperationMode, UserPreferences}
import lib.Validation._
import net.ripe.rpki.validator.lib.RoaOperationMode.RoaOperationMode


class UserPreferencesView(val userPreferences: UserPreferences, val messages: Seq[FeedbackMessage] = Seq.empty) extends View with ViewHelpers {

  private val fieldNameToText = Map("enable-update-checks" -> "Check for updates", "max-stale-days" -> "Maximum days out of date")

  private def isRoaOperationMode(roaOperationMode: RoaOperationMode): Boolean = {
    if (roaOperationMode.equals(userPreferences.roaOperationMode)) {
      return true
    }
    else {
      return false
    }
  }

  def tab = Tabs.UserPreferencesTab

  def title = Text("User Preferences")

  def body = {

    <div>
      {renderMessages(messages, fieldNameToText)}
    </div>
      <div class="well">
        <form method="POST" class="form-stacked">
          <fieldset>
            <div>
              <label class="checkbox">
                {userPreferences.updateAlertActive match {
                case true => <input name="enable-update-checks" type="checkbox" checked="checked"/>
                case false => <input name="enable-update-checks" type="checkbox"/>
              }}
                Automatically check for new versions of this application
              </label>
              <label class="checkbox">
                Accept repositories that are no longer updated for up to
                <span rel="twipsy" data-original-title="Increasing this number means that you are less strict about out-of-date repositories, but this means you are more vulnerable to replay attacks. If in doubt, you can leave this on the default setting of 0.">
                  <input type="number" class="span2" min="0" name="max-stale-days" value={Text(userPreferences.maxStaleDays.toString)}/>
                </span>
                days.
              </label>
              <label class="radio">
                Current protection level is:
                {Text(userPreferences.roaOperationMode.toString)}
                choose protection level -
                {if (isRoaOperationMode(RoaOperationMode.ManualMode)) {
                <ul>
                  <li>
                    <input type="radio" name="ROA-operation-mode" value={Text(RoaOperationMode.ManualMode.toString)} checked="checked"/>
                    Manual mode</li>
                  <li>
                    <input type="radio" name="ROA-operation-mode" value={Text(RoaOperationMode.AutoModeRemoveBadROA.toString)}/>
                    Automatically remove ROA's which invalidate BGP anonucments</li>
                  <li>
                    <input type="radio" name="ROA-operation-mode" value={Text(RoaOperationMode.AutoModeRemoveGoodROA.toString)}/>
                    Automatically preserve ROA's which invalidate BGP anonucments</li>
                </ul>
              } else if (isRoaOperationMode(RoaOperationMode.AutoModeRemoveBadROA)) {
                <ul>
                  <li>
                    <input type="radio" name="ROA-operation-mode" value={Text(RoaOperationMode.ManualMode.toString)}/>
                    Manual mode</li>
                  <li>
                    <input type="radio" name="ROA-operation-mode" value={Text(RoaOperationMode.AutoModeRemoveBadROA.toString)} checked="checked"/>
                    Automatically remove ROA's which invalidate BGP anonucments</li>
                  <li>
                    <input type="radio" name="ROA-operation-mode" value={Text(RoaOperationMode.AutoModeRemoveGoodROA.toString)}/>
                    Automatically preserve ROA's which invalidate BGP anonucments</li>
                </ul>
              }
              else {
                <ul>
                  <li>
                    <input type="radio" name="ROA-operation-mode" value={Text(RoaOperationMode.ManualMode.toString)}/>
                    Manual mode</li>
                  <li>
                    <input type="radio" name="ROA-operation-mode" value={Text(RoaOperationMode.AutoModeRemoveBadROA.toString)}/>
                    Automatically remove ROA's which invalidate BGP anonucments</li>
                  <li>
                    <input type="radio" name="ROA-operation-mode" value={Text(RoaOperationMode.AutoModeRemoveGoodROA.toString)} checked="checked"/>
                    Automatically preserve ROA's which invalidate BGP anonucments</li>
                </ul>

              }}
              </label>
              <label class="checkbox">
                {userPreferences.roaBgpConflictLearnMode match {
                case true => <input name="BGP-ROA-CONFLICT-LEARN-MODE" type="checkbox" checked="checked"/>
                case false => <input name="BGP-ROA-CONFLICT-LEARN-MODE" type="checkbox"/>
              }}
                Enable to learn ROA - BGP Anoucment conflict and automatically approve conflicted BGP Anouncmetns after specific amount of time.
                <span rel="twipsy" data-original-title="Increasing this number increases the certinty rate that a BGP-ROA conflict is a mistake">
                  time:
                  <input type="number" class="span2" min="0" name="fake-conflict-certinty-rate" value={Text(userPreferences.conflictCertDays.toString)}/>
                </span>
              </label>
              <label class="checkbox">
                The number of days to hold BGP-ROA conflicted values that weren't validated
                <span rel="twipsy" data-original-title="Increasing this number means that you would like to hold ROA-BGP conflicted that weren't validated fot this number of days.">
                  <input type="number" class="span2" min="0" name="max-conflicted-bgp-stale-days" value={Text(userPreferences.maxConflictedBgpStaleDays.toString)}/>
                </span>
                days.
              </label>
            </div>
            <div>
              <br/>
              <button type="submit" class="btn primary">Update Preferences</button>
            </div>
          </fieldset>
        </form>
      </div>
      <script>
        <!--
$(function() {
     var $radios = $('input:radio[name=gender]');
    if($radios.is(':checked') === false) {
        $radios.filter('[value=Male]').prop('checked', true);
    }
});
$(function () {
  $('[rel=twipsy]').twipsy({
    "live": true
  });
});
//-->
      </script>
  }
}
