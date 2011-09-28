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
package rtr

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import java.nio.ByteOrder
import scala.util.Random

import net.ripe.ipresource._

sealed trait Pdu {
  def protocolVersion: Byte = 0
  def pduType: Byte
  def headerShort: Short
  def length: Int
}

case class BadData(errorCode: Int, content: Array[Byte])

/**
 * See: http://tools.ietf.org/html/draft-ietf-sidr-rpki-rtr-16#section-5.3
 */
case class ResetQueryPdu() extends Pdu {
  override def pduType = PduTypes.ResetQuery
  override def headerShort: Short = 0
  override def length = 8
}

/**
 * See: http://tools.ietf.org/html/draft-ietf-sidr-rpki-rtr-16#section-5.4
 */
case class CacheResponsePdu(nonce: Short = (new Random().nextInt() % Short.MaxValue).toShort) extends Pdu {
  override def pduType = PduTypes.CacheResponse
  override def headerShort: Short = nonce
  override def length = 8
}

/**
 * See: http://tools.ietf.org/html/draft-ietf-sidr-rpki-rtr-16#section-5.5
 */
case class IPv4PrefixAnnouncePdu(val ipv4PrefixStart: Ipv4Address, val prefixLength: Byte, val maxLength: Byte, val asn: Asn) extends Pdu {

  override def pduType = PduTypes.IPv4Prefix
  override def headerShort: Short = 0
  override def length = 20

}

case class ErrorPdu(errorCode: Int, causingPdu: Array[Byte], errorText: String) extends Pdu {
  final override val pduType = PduTypes.Error
  override def headerShort = errorCode.toShort

  def causingPduLength = causingPdu.length

  val errorTextBytes: Array[Byte] = errorText.getBytes("UTF-8")
  val errorTextLength = errorTextBytes.length

  override val length = 8 + 4 + causingPduLength + 4 + errorTextLength
}

object ErrorPdu {
  val CorruptData = 0
  val InternalError = 1
  val NoDataAvailable = 2
  val InvalidRequest = 3
  val UnsupportedProtocolVersion = 4
  val UnsupportedPduType = 5
  val WithdrawalOfUnkownRecord = 6
  val DuplicateAnnouncementReceived = 7

  def isFatal(errorCode: Int) = errorCode != NoDataAvailable

}

object PduTypes {
  val ResetQuery: Byte = 2
  val CacheResponse: Byte = 3
  val IPv4Prefix: Byte = 4
  val Error: Byte = 10
}

object Pdus {
  val SupportedProtocol: Byte = 0

  def encode(pdu: Pdu): Array[Byte] = {
    val buffer = ChannelBuffers.buffer(ByteOrder.BIG_ENDIAN, pdu.length)
    buffer.writeByte(pdu.protocolVersion)
    buffer.writeByte(pdu.pduType)
    buffer.writeShort(pdu.headerShort)
    buffer.writeInt(pdu.length)

    pdu match {
      case errorPdu @ ErrorPdu(errorCode, causingPdu, errorText) =>
        buffer.writeInt(causingPdu.length)
        buffer.writeBytes(causingPdu)
        buffer.writeInt(errorPdu.errorTextBytes.length)
        buffer.writeBytes(errorPdu.errorTextBytes)
      case ResetQueryPdu() =>
      case CacheResponsePdu(_) =>
      case IPv4PrefixAnnouncePdu(prefix, length, maxLength, asn) =>
        buffer.writeByte(1)
        buffer.writeByte(length)
        buffer.writeByte(maxLength)
        buffer.writeByte(0)
        buffer.writeInt(prefix.longValue().toInt)
        buffer.writeInt(asn.longValue().toInt)
    }

    buffer.array()
  }

  def fromByteArray(buffer: ChannelBuffer): Either[BadData, Pdu] = try {
    val protocol = buffer.readByte()
    val pduType = buffer.readByte()
    val headerShort = buffer.readUnsignedShort().toShort
    val length = buffer.readInt()

    if (protocol != SupportedProtocol) {
      Left(BadData(ErrorPdu.UnsupportedProtocolVersion, buffer.array))
    } else {
      pduType match {
        case PduTypes.Error =>
          val causingPduLength = buffer.readInt()
          val causingPdu = buffer.readBytes(causingPduLength).array()
          val errorTextLength = buffer.readInt()
          val errorTextBytes = buffer.readBytes(errorTextLength)
          val errorText = new String(buffer.array(), "UTF-8")
          Right(ErrorPdu(headerShort, causingPdu, errorText))
        case PduTypes.ResetQuery =>
          Right(ResetQueryPdu())
        case PduTypes.CacheResponse =>
          val nonce = headerShort
          Right(CacheResponsePdu(nonce))
        case PduTypes.IPv4Prefix =>
          buffer.readByte() match {
            case 1 =>
              val length = buffer.readByte()
              val maxLenght = buffer.readByte()
              buffer.skipBytes(1)
              val prefix = new Ipv4Address(buffer.readInt())
              val asn = new Asn(buffer.readInt())
              Right(IPv4PrefixAnnouncePdu(prefix, length, maxLenght, asn))
            case _ =>
              Left(BadData(ErrorPdu.UnsupportedPduType, buffer.array))
          }
        case _ =>
          Left(BadData(ErrorPdu.UnsupportedPduType, buffer.array))
      }
    }
  } catch {
    case e: IndexOutOfBoundsException =>
      Left(BadData(ErrorPdu.CorruptData, buffer.array()))
  }
}

