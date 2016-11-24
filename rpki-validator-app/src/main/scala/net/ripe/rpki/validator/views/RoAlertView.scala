package net.ripe.rpki.validator.views

import java.net.UnknownHostException

import net.ripe.ipresource.IpRange

class RoAlertView(Unit: Unit) {
  def tab = Tabs.RoAlertTab
  def title = tab.text
  def body = {

    <div>
      Your IpAddress is: { getHostIp.toString() }
    </div>
  }


  private def getHostIp: IpRange = {
    try {
      val addr = scala.io.Source.fromURL("https://api.ipify.org").mkString
      IpRange.parse(addr)
    }   catch {
      case e: Exception =>
      val unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e)
      unknownHostException.initCause(e)
      throw unknownHostException
    }



  }



}
