package io.ncats.translator.beacon

import com.typesafe.config.ConfigFactory

import Beacon.modelMarshaller
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import scala.collection.JavaConverters._
import Beacon.resourceUnmarshaller
import org.apache.jena.rdf.model.Resource
import org.prefixcommons.CurieUtil

object Main extends HttpApp with App {

  val conf = ConfigFactory.load()
  val hostname = conf.getString("beacon.tfp.hostname")
  val port = conf.getInt("beacon.tfp.port")
  val prefixes = new CurieUtil(conf.getObject("beacon.tfp.prefixes").entrySet().asScala
    .map(e => e.getKey -> e.getValue.unwrapped.toString).toMap.asJava)
  lazy val system = this.systemReference.get

  def route: Route = cors() {
    pathPrefix("beacon") {
      pathPrefix(Segment) { beaconEndpoint =>
        path("pattern") {
          parameters('s.as[Resource].?, 'p.as[Resource].?, 'o.as[Resource].?) { (s, p, o) =>
            get {
              complete {
                new Beacon(beaconEndpoint).query(s, p, o)
              }
            }
          }
        }
      }
    }
  }

  // Starting the server
  Main.startServer(hostname, port)

}


