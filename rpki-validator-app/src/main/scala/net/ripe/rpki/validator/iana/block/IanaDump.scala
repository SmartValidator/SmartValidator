package net.ripe.rpki.validator.iana.block

import java.io.{BufferedReader, InputStream, InputStreamReader}

import grizzled.slf4j.Logging
import net.ripe.ipresource.{Asn, IpRange}
import net.ripe.rpki.validator.lib.DateAndTime
import org.joda.time.DateTime

import scala.util.control.NonFatal

/**
  * Created by fimka on 16/10/16.
  */

case class IanaEntry(origin: Asn, prefix: IpRange, visibility: Int)
case class IanaDump(url: String, lastModified: Option[DateTime] = None, entries: Seq[IanaEntry] = Nil)

object IanaDump extends Logging {
  def toAnnouncedRoutes(entries: Seq[IanaEntry]) = {
    val (r, t) = DateAndTime.timed {
      entries.par.
        filter(e => e.visibility >= IanaAnnouncementValidator.VISIBILITY_THRESHOLD).
        map(e => IanaAnnouncement(e.origin, e.prefix)).distinct.seq
    }
    info(s"toAnnouncedRoutes time ${t/1000.0} seconds")
    r
  }

  def parse(is: InputStream): Either[Exception, IndexedSeq[IanaEntry]] = {
    val identityMap = new ObjectIdentityMap
    val reader = new BufferedReader(new InputStreamReader(is))
    try {
      Right(Iterator.continually(reader.readLine()).takeWhile(_ != null).flatMap(parseLine).map(makeResourcesUnique(identityMap, _)).toIndexedSeq)
    } catch {
      case e: Exception =>
        Left(e)
    }
  }

  private def makeResourcesUnique(identityMap: ObjectIdentityMap, entry: IanaEntry) = {
    import identityMap._
    val asn = makeUnique(entry.origin)
    val prefix = (makeUnique(entry.prefix.getStart) upTo makeUnique(entry.prefix.getEnd)).asInstanceOf[IpRange]
    entry.copy(origin = asn, prefix = prefix)
  }

  private val IanaEntryRegex = """^\s*([0-9]+)\s+([0-9a-fA-F.:/]+)\s+([0-9]+)\s*$""".r
  private[block] def parseLine(content: String): Option[IanaEntry] = {
    content match {
      case IanaEntryRegex(asn, ipprefix, visibility) =>
        try {
          Some(IanaEntry(origin = Asn.parse(asn), prefix = IpRange.parse(ipprefix), visibility = Integer.parseInt(visibility)))
        } catch {
          case NonFatal(e) =>
            error("Skipping unparseble line: " + content)
            debug("Detailed error: ", e)
            None
        }
      case _ => None
    }
  }

  private[this] class ObjectIdentityMap {
    private val identityMap = collection.mutable.Map.empty[AnyRef, AnyRef]

    def makeUnique[A <: AnyRef](a: A): A = identityMap.getOrElseUpdate(a, a).asInstanceOf[A]
  }
}
