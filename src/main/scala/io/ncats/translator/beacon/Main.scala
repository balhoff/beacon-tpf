package io.ncats.translator.beacon

import com.typesafe.config.ConfigFactory

import Beacon.modelMarshaller
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

object Main extends HttpApp with App {

  val conf = ConfigFactory.load()
  val hostname = conf.getString("beacon.tfp.hostname")
  val port = conf.getInt("beacon.tfp.port")
  val beacon = conf.getString("beacon.tfp.beacon")
  lazy val system = this.systemReference.get

  def route: Route = cors() {
    path("pattern") {
      parameters('s.?, 'p.?, 'o.?) { (s, p, o) =>
        get {
          complete {
            new Beacon(beacon).query(s, p, o)
          }
        }
      }
    }
  }

  // Starting the server
  Main.startServer(hostname, port)

}


