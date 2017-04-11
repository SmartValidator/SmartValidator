package net.ripe.rpki.validator.models
import net.ripe.ipresource.{Asn, IpRange}

/**
  * Created by raphael on 21/03/17.
  */

case class LocalPathEndNeighbors(var entries: scala.collection.mutable.Set[Asn] = scala.collection.mutable.Set.empty) {
  def addPathEndNeighbor(entry: Asn) = copy(entries + entry)

  def removePathEndNeighbor(entry: Asn) = copy(entries - entry)

}