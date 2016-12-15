package net.ripe.rpki.validator.views

import net.ripe.rpki.validator.lib.Validation.FeedbackMessage
import net.ripe.rpki.validator.models.{LooseRoa, RoaBlackList}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by fimka on 08/12/16.
  */
class RoaBlackListView (roaBlackList: RoaBlackList, looseRoas:ArrayBuffer[LooseRoa], params: Map[String, String] = Map.empty,
                        messages: Seq[FeedbackMessage] = Seq.empty) extends View with ViewHelpers {
    private val fieldNameToText = Map("prefix" -> "Prefix")

    def tab = Tabs.RoaBlackList
    def title = tab.text
    def body = {
      <div>
        { renderMessages(messages, fieldNameToText) }
        <table>
          <thead>
            <tr><th>ASN</th><th>Prefix</th><th>Max prefix</th></tr>
          </thead>
          {
          if(looseRoas.isEmpty)
          {
            <h4> No VALID ROA's were found for this IP </h4>
          }
          else
          {
            for ( releventRoa <- looseRoas ) yield {
              <tr>
                <td> { releventRoa.asn.toString } </td>
                <td> { releventRoa.prefix.toString} </td>
              </tr>
            }
          }

          }
        </table>
      </div>
    }
}
