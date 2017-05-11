package io.ncats.translator.beacon

import spray.json._

object BeaconStatements {

  case class BeaconResource(id: String, name: String)

  case class BeaconStatement(id: String, subject: BeaconResource, predicate: BeaconResource, `object`: BeaconResource)

  object BeaconResourceProtocol extends DefaultJsonProtocol {

    implicit val BeaconResourceFormat = jsonFormat2(BeaconResource.apply)
  }

  object BeaconStatementProtocol extends DefaultJsonProtocol {

    import BeaconResourceProtocol.BeaconResourceFormat

    implicit val BeaconStatementFormat = jsonFormat4(BeaconStatement.apply)
  }

}

