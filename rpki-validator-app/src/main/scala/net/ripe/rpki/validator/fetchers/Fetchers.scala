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
package net.ripe.rpki.validator.fetchers

import java.io.{File, PrintWriter}
import java.net.URI
import java.nio.file.Files

import com.google.common.io.BaseEncoding
import net.ripe.rpki.commons.crypto.cms.manifest.ManifestCmsParser
import net.ripe.rpki.commons.crypto.cms.roa.RoaCms
import net.ripe.rpki.commons.crypto.crl.X509Crl
import net.ripe.rpki.commons.crypto.x509cert.X509ResourceCertificateParser
import net.ripe.rpki.commons.rsync.Rsync
import net.ripe.rpki.validator.config.{Http, ApplicationOptions}
import net.ripe.rpki.validator.models.validation._
import net.ripe.rpki.validator.store.HttpFetcherStore
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet}
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.log4j.Logger

import scala.collection.JavaConversions._
import scala.collection.immutable
import scala.util.Try
import scala.xml.{NodeSeq, Elem}

case class FetcherConfig(rsyncDir: String = "")

trait Fetcher {
  type Callback = Either[BrokenObject, RepositoryObject[_]] => Unit
  def fetchRepo(uri: URI)(process: Callback): Seq[String]
}

class RsyncFetcher(config: FetcherConfig) extends Fetcher {

  private val logger: Logger = Logger.getLogger(classOf[RsyncFetcher])

  private val OPTIONS = Seq("--update", "--times", "--copy-links", "--recursive", "--delete")

  private def walkTree[T](d: File)(f: File => Option[T]): Seq[T] = {
    if (d.isDirectory) {
      d.listFiles.map(walkTree(_)(f)).toSeq.flatten
    } else f(d) match {
      case Some(x) => Seq(x)
      case None => Seq()
    }
  }

  private[this] def withRsyncDir[T](uri: URI)(f: File => T) = {
    def uriToPath = uri.toString.replaceAll("rsync://", "")
    def destDir = {
      val rsyncPath = new File(config.rsyncDir + "/" + uriToPath)
      if (!rsyncPath.exists) {
        rsyncPath.mkdirs
      }
      rsyncPath
    }

    f(destDir)
  }

  def rsyncMethod(uri: URI, destDir: File) = {
    val r = new Rsync(uri.toString, destDir.getAbsolutePath)
    r.addOptions(OPTIONS)
    try {
      r.execute match {
        case 0 => Seq()
        case code => Seq( s"""Returned code: $code, stderr: ${r.getErrorLines.mkString("\n")}""")
      }
    } catch {
      case e: Exception => Seq( s"""Failed with exception, ${e.getMessage}""")
    }
  }

  override def fetchRepo(uri: URI)(process: Callback): Seq[String] = fetchRepo(uri, rsyncMethod)(process)

  def fetchRepo(uri: URI, method: (URI, File) => Seq[String])(process: Callback): Seq[String] = withRsyncDir(uri) {
    destDir =>
      logger.info(s"Downloading the repository $uri to ${destDir.getAbsolutePath}")
      method(uri, destDir) ++ readObjects(destDir, uri, process)
  }

  def readObjects(tmpRoot: File, repoUri: URI, process: Callback): Seq[String] = {
    val replacement = {
      val s = repoUri.toString
      if (s.endsWith("/")) s.dropRight(1) else s
    }

    def rsyncUrl(f: File) =
      if (replacement.endsWith(f.getName))
        replacement
      else
        f.getAbsolutePath.replaceAll(tmpRoot.getAbsolutePath, replacement)

    walkTree(tmpRoot) {
      f =>
        val extension = f.getName.takeRight(3).toLowerCase
        var error: Option[String] = None
        val obj = extension match {
          case "cer" => process(CertificateObject.tryParse(rsyncUrl(f), readFile(f)))
          case "mft" => process(ManifestObject.tryParse(rsyncUrl(f), readFile(f)))
          case "crl" => process(CrlObject.tryParse(rsyncUrl(f), readFile(f)))
          case "roa" => process(RoaObject.tryParse(rsyncUrl(f), readFile(f)))
          case "gbr" => error = Some("We don't support GBR records yet")
          case _ => error = Some(s"Found unknown file $f")
        }
        error
    }
  }

  private def readFile(f: File) = Files.readAllBytes(f.toPath)
}

class HttpFetcher(config: FetcherConfig, store: HttpFetcherStore) extends Fetcher with Http {

  case class PublishUnit(uri: String, hash: String, base64: String)

  case class WithdrawUnit(uri: String, hash: String)

  case class NotificationDef(sessionId: String, serial: BigInt)

  case class SnapshotDef(url: String, hash: String)

  case class DeltaDef(serial: BigInt, url: String, hash: String)

  case class Delta(deltaDef: DeltaDef, publishes: Seq[PublishUnit], withdraw: Seq[WithdrawUnit] = Seq())

  case class Snapshot(snapshotDef: SnapshotDef, publishes: Seq[PublishUnit], withdraw: Seq[WithdrawUnit] = Seq())

  case class Error(url: URI, message: String)

  private val base64 = BaseEncoding.base64()

  override def fetchRepo(notificationUri: URI)(process: Callback): Seq[String] = {

    val notificationXml = getXml(notificationUri)

    val notificationDef = notificationXml.right.flatMap { xml =>
      try {
        Right(NotificationDef((xml \ "@session_id").text, BigInt((xml \ "@serial").text)))
      } catch {
        case e: NumberFormatException => Left(Error(notificationUri, "Couldn't parse serial number"))
        case e: Throwable => Left(Error(notificationUri, s"Error: ${e.getMessage}"))
      }
    }

    val snapshotDef = notificationXml.right.flatMap { xml =>
      (xml \ "snapshot").map(x => ((x \ "@uri").text, (x \ "@hash").text)) match {
        case Seq(s) => Right(SnapshotDef(s._1, s._2))
        case _ => Left(Error(notificationUri, "There should one and only one 'snapshot' element'"))
      }
    }

    val snapshotContent: Either[Error, Snapshot] = snapshotDef.right.flatMap { sd => fetchSnapshot(new URI(sd.url), sd) }

    val requiredDeltas = notificationXml.right.map { xml =>
      val deltas = (xml \ "delta").map(x => DeltaDef(BigInt((x \ "@serial").text), (x \ "@uri").text, (x \ "@hash").text))
      notificationDef.right.map { nd =>
        val lastSerial = store.getSerial(notificationUri.toString, nd.sessionId).getOrElse(BigInt(0L))
        deltas.filter(_.serial > lastSerial).sortBy(_.serial)
      }
    }.joinRight

    val deltaContents: Either[Error, Seq[Either[Error, Delta]]] = requiredDeltas.right.map {
      deltas => deltas.map { d => fetchDelta(new URI(d.url), d) }
    }

    // process snapshot content
    val x = snapshotContent.right.map { s =>
      s.publishes.map { parsePublishUnit(_, process) }
    }

    // process deltas content
    val map: Either[Error, Seq[Either[Error, Seq[Option[String]]]]] = deltaContents.right.map { dcs: Seq[Either[Error, Delta]] =>
      dcs.map { e: Either[Error, Delta] =>
        e.right.map { d: Delta =>
          d.publishes.map(parsePublishUnit(_, process))
        }
      }
    }

    map

    // gather errors

    Seq()

  }

  private def parsePublishUnit(p: PublishUnit, process: Callback): Option[String] = {
    val extension = p.uri.takeRight(3).toLowerCase
    var error: Option[String] = None

    def decodeBase64 = try {
      base64.decode(p.base64.filterNot(Character.isWhitespace))
    } catch {
      case e: Throwable =>
        error = Some(e.getMessage)
        Array[Byte]()
    }

    val obj = extension match {
      case "cer" => process(CertificateObject.tryParse(p.uri, decodeBase64))
      case "mft" => process(ManifestObject.tryParse(p.uri, decodeBase64))
      case "crl" => process(CrlObject.tryParse(p.uri, decodeBase64))
      case "roa" => process(RoaObject.tryParse(p.uri, decodeBase64))
      case "gbr" => error = Some("We don't support GBR records yet")
      case _ => error = Some(s"Found unknown URI type ${p.uri}")
    }
    error
  }

  def getXml(notificationUri: URI): Either[Error, Elem] = {
    try {
      val http1: CloseableHttpClient = http
      val response = http1.execute(new HttpGet(notificationUri))
      Right(scala.xml.XML.load(response.getEntity.getContent))
    } catch {
      case e: Exception => Left(Error(notificationUri, e.getMessage))
    }
  }

  private def fetchSnapshot(snapshotUrl: URI, snapshotDef: SnapshotDef): Either[Error, Snapshot] =
    getPublishUnits(snapshotUrl, Snapshot(snapshotDef, _))

  private def fetchDelta(deltaUrl: URI, deltaDef: DeltaDef): Either[Error, Delta] =
    getPublishUnits(deltaUrl, Delta(deltaDef, _))

  private def getPublishUnits[T](url: URI, f: Seq[PublishUnit] => T): Either[Error, T] = {
    getXml(url).right.flatMap { xml =>
      val publishes = (xml \ "publish").map(x => PublishUnit((x \ "@uri").text, (x \ "@hash").text, x.text))
      if (publishes.exists {
        p => Option(p.uri).exists(_.isEmpty) &&
          Option(p.hash).exists(_.isEmpty) &&
          Option(p.base64).exists(_.isEmpty)
      }) {
        // TODO Make it better
        Left(Error(url, "Mandatory attributes are absent"))
      }
      else
        Right(f(publishes))
    }
  }

}

