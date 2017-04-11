package net.ripe.rpki.validator.models

import net.ripe.ipresource.{Asn, IpRange}

/**
  * Created by raphael on 09/03/17.
  */

case class PathEndRecord(originAsn: Asn, var trustedAsns:  scala.collection.mutable.Set[Asn] = scala.collection.mutable.Set.empty,timeStamp: String) {

}

case class PathEndTable(var entries: scala.collection.mutable.Set[PathEndRecord] = scala.collection.mutable.Set.empty) {
  def addPathEndRecord(record: PathEndRecord) = copy(entries + record)

  def removePathEndRecord(record: PathEndRecord) = copy(entries - record)

}
  //Add shouldIgnore an sanity cheks for add and remove

