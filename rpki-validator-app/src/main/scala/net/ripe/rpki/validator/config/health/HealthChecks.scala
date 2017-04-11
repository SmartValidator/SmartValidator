/**
 * The BSD License
 *
 * Copyright (c) 2010-2012 RIPE NCC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   - Neither the name of the RIPE NCC nor the names of its contributors may be
 *     used to endorse or promote products derived from this software without
 *     specific prior written permission.
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
package net.ripe.rpki.validator.config.health

import net.ripe.rpki.commons.rsync.Rsync

trait HealthCheck {
  def check(): Status
}

object Code extends Enumeration {
  type Code = Value
  val OK = Value("OK")
  val WARNING = Value("WARNING")
  val ERROR = Value("ERROR")
}

case class Status(code: Code.Code, message: Option[String])

object Status {
  def ok = Status(Code.OK, None)
  def ok(message: String) = Status(Code.OK, Some(message))
  def warning(message: String) = Status(Code.WARNING, Some(message))
  def error(message: String) = Status(Code.ERROR, Some(message))
}

object HealthChecks {
  def registry =
    Map("rsync" -> new RsyncHealthCheck)
}

class RsyncHealthCheck extends HealthCheck {

  override def check() = try {
    val rsync = new Rsync
    rsync.addOptions("--version")
    val rc = rsync.execute()
    if (rc == 0)
      Status.ok("can find and execute rsync")
    else
      Status.error("problems executing rsync, make sure you have rsync installed on the path")
  } catch {
    case e: Exception =>
      Status.error(e.getMessage)
  }

}
