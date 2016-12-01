package net.ripe.rpki.validator.views

import java.net.UnknownHostException

import net.ripe.ipresource.IpAddress
import net.ripe.rpki.validator.models.{RtrPrefix, ValidatedObjects}

class RoAlertView(validatedObjects: ValidatedObjects) extends View with ViewHelpers {
  val allRecords = validatedObjects.getValidatedRtrPrefixes.toIndexedSeq

  def tab = Tabs.RoAlertTab
  def title = tab.text
  def body = {
    val externalIp = getHostIp
    <div>

      Your IpAddress is: { externalIp.toString() }
      Relevent roa:
      <table>
        <thead>
          <tr><th>ASN</th><th>Prefix</th><th>Max prefix</th></tr>
        </thead>
        {
        for { relventRoa <- filterRelativeIp(externalIp) } yield {
          <tr>
            <td> { relventRoa.asn.toString } </td>
            <td> { relventRoa.prefix.toString } </td>
            <td> { relventRoa.maxPrefixLength.toString } </td>

          </tr>
        }
        }
      </table>
    </div>
  }


  private def filterRelativeIp(ipAddress: IpAddress): IndexedSeq[RtrPrefix] = {
    allRecords.filter(_.prefix.contains(ipAddress))
  }



  private def getHostIp: IpAddress = {
    try {
      val addr = scala.io.Source.fromURL("https://api.ipify.org").mkString
      IpAddress.parse(addr)
    }   catch {
      case e: Exception =>
      val unknownHostException = new UnknownHostException("Failed to determine IP address: " + e)
      unknownHostException.initCause(e)
      throw unknownHostException
    }



  }



}
