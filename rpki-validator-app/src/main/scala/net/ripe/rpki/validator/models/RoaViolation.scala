package net.ripe.rpki.validator.models

/**
  * Created by fimka on 08/12/16.
  */
object RoaViolation {
    sealed trait EnumVal
    case object LooseRoa extends EnumVal

    val daysOfWeek = Seq(LooseRoa)

  }
