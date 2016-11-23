package net.ripe.rpki.validator.views

import java.net.{NetworkInterface, UnknownHostException}

import net.ripe.ipresource.IpResource

class RoAlertView(Unit: Unit) {
  def tab = Tabs.RoAlertTab
  def title = tab.text
  def body = {}


  private def getHostIp: (Seq[IpResource]) => List[IpResource] = {
    try {
      val interfaceEnumerator = new scala.collection.JavaConversions.JEnumerationWrapper(NetworkInterface.getNetworkInterfaces())
      for(name <- interfaceEnumerator)
        {
          val inetAddressEnumerator = new scala.collection.JavaConversions.JEnumerationWrapper(name.getInetAddresses)
          for(inetAddress <- inetAddressEnumerator)
            {
              if()
                {

                }

            }
        }


//        NetworkInterface iface = (NetworkInterface) ifaces.get;
//        // Iterate all IP addresses assigned to each card...
//        for (util.Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
//          InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
//          if (!inetAddr.isLoopbackAddress()) {
//
//            if (inetAddr.isSiteLocalAddress()) {
//              // Found non-loopback site-local address. Return it immediately...
//              return inetAddr;
//            }
//            else if (candidateAddress == null) {
//              // Found non-loopback address, but not necessarily site-local.
//              // Store it as a candidate to be returned if site-local address is not subsequently found...
//              candidateAddress = inetAddr;
//              // Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
//              // only the first. For subsequent iterations, candidate will be non-null.
//            }
//          }
//        }
//      }
//      if (candidateAddress != null) {
//        // We did not find a site-local address, but we found some other non-loopback address.
//        // Server might have a non-site-local address assigned to its NIC (or it might be running
//        // IPv6 which deprecates the "site-local" concept).
//        // Return this non-loopback candidate address...
//        return candidateAddress;
//      }
//      // At this point, we did not find a non-loopback address.
//      // Fall back to returning whatever InetAddress.getLocalHost() returns...
//      InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
//      if (jdkSuppliedAddress == null) {
//        throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");

//      return jdkSuppliedAddress;
      List[IpResource]
    }
    catch {
      case e: Exception =>
      val unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e)
      unknownHostException.initCause(e)
      throw unknownHostException
    }



  }



}
