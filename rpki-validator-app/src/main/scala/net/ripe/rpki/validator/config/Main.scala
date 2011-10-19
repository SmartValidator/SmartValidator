/**
 * The BSD License
 *
 * Copyright (c) 2010, 2011 RIPE NCC
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
package net.ripe.rpki.validator
package config

import java.io.File

import scala.collection.JavaConverters._

import org.apache.commons.io.FileUtils
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.FilterMapping
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder

import grizzled.slf4j.Logger
import java.{ util => ju }
import net.ripe.certification.validator.util.TrustAnchorExtractor
import scalaz.concurrent.Promise

import lib._
import models._
import net.ripe.rpki.validator.rtr.Pdu
import net.ripe.rpki.validator.rtr.RTRServer

object Main {

  val logger = Logger[this.type]

  private val nonce: Pdu.Nonce = Pdu.randomNonce()

  private var memoryImage: Atomic[MemoryImage] = null
  private var memoryImageListener: Option[MemoryImage => Unit] = None

  def main(args: Array[String]): Unit = Options.parse(args) match {
    case Right(options) => run(options)
    case Left(message) => error(message)
  }

  private def run(options: Options): Unit = {
    val trustAnchors = loadTrustAnchors()
    val roas = Roas(trustAnchors)
    val dataFile = new File(options.dataFileName).getCanonicalFile()
    val data = PersistentDataSerialiser.read(dataFile).getOrElse(PersistentData(whitelist = Whitelist()))
    memoryImage = new Atomic(
      MemoryImage(data.whitelist, trustAnchors, roas),
      memoryImage => for (listener <- memoryImageListener) listener(memoryImage))

    runWebServer(options, dataFile)
    runRtrServer(options)
  }

  private def error(message: String) = {
    println(message)
    sys.exit(1)
  }

  def loadTrustAnchors(): TrustAnchors = {
    import java.{ util => ju }
    val tals = new ju.ArrayList(FileUtils.listFiles(new File("conf/tal"), Array("tal"), false).asInstanceOf[ju.Collection[File]])
    val trustAnchors = TrustAnchors.load(tals.asScala, "tmp/tals")
    for (ta <- trustAnchors.all) {
      Promise {
        val certificate = new TrustAnchorExtractor().extractTA(ta.locator, "tmp/tals")
        logger.info("Loaded trust anchor from location " + certificate.getLocation())
        memoryImage.update {
          db => db.updateTrustAnchor(ta.locator, certificate)
        }
        val validatedRoas = Roas.fetchObjects(ta.locator, certificate)
        memoryImage.update {
          db => db.updateRoas(ta.locator, validatedRoas)
        }
      }
    }
    trustAnchors
  }

  def setup(server: Server, dataFile: File): Server = {
    import org.eclipse.jetty.servlet._
    import org.scalatra._

    val root = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS)
    root.setResourceBase(getClass().getResource("/public").toString())
    val defaultServletHolder = new ServletHolder(new DefaultServlet())
    defaultServletHolder.setName("default")
    defaultServletHolder.setInitParameter("dirAllowed", "false")
    root.addServlet(defaultServletHolder, "/*")
    root.addFilter(new FilterHolder(new WebFilter {
      override def trustAnchors = memoryImage.get.trustAnchors

      override def roas = memoryImage.get.roas

      override def version = memoryImage.get.version

      override def lastUpdateTime = memoryImage.get.lastUpdateTime

      override def whitelist = memoryImage.get.whitelist

      override def addWhitelistEntry(entry: WhitelistEntry) = memoryImage synchronized {
        memoryImage.update(_.addWhitelistEntry(entry))
        PersistentDataSerialiser.write(PersistentData(whitelist = memoryImage.get.whitelist), dataFile)
      }

      override def removeWhitelistEntry(entry: WhitelistEntry) = memoryImage synchronized {
        memoryImage.update(_.removeWhitelistEntry(entry))
        PersistentDataSerialiser.write(PersistentData(whitelist = memoryImage.get.whitelist), dataFile)
      }
    }), "/*", FilterMapping.ALL)
    server.setHandler(root)
    server
  }

  private def runWebServer(options: Options, dataFile: File): Unit = {
    val server = setup(new Server(options.httpPort), dataFile)

    sys.addShutdownHook({
      server.stop()
      logger.info("Bye, bye...")
    })
    server.start()
    logger.info("Welcome to the RIPE NCC RPKI Validator, now available on port " + options.httpPort + ". Hit CTRL+C to terminate.")
  }

  private def runRtrServer(options: Options): Unit = {
    var rtrServer = new RTRServer(port = options.rtrPort, noCloseOnError = options.noCloseOnError, noNotify = options.noNotify, getCurrentCacheSerial = {
      () => memoryImage.get.version
    }, getCurrentRoas = {
      () => memoryImage.get.roas
    }, getCurrentNonce = {
      () => Main.nonce
    })
    rtrServer.startServer()
    memoryImageListener = Some(memoryImage => rtrServer.notify(memoryImage.version))
  }

}
