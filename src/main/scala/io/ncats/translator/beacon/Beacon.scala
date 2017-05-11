package io.ncats.translator.beacon

import java.io.StringWriter
import java.net.URLEncoder

import scala.concurrent.Future

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.rdf.model.Statement
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFFormat
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.VOID

import BeaconStatements.BeaconStatement
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.http.scaladsl.unmarshalling.Unmarshaller.stringUnmarshaller
import akka.stream.ActorMaterializer
import spray.json._

class Beacon(endpoint: String) {

  val Hydra = "http://www.w3.org/ns/hydra/core"
  implicit val system = Main.system
  implicit val executionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()
  import BeaconStatements.BeaconStatementProtocol._

  def query(s: Option[Resource], p: Option[Resource], o: Option[Resource]): Future[Model] = {
    s.orElse(o) match {
      case None => Future.failed(new IllegalRequestException(
        ErrorInfo("Either subject or object must be provided.", "Knowledge beacons do not support query by predicate only."),
        StatusCodes.BadRequest))
      case Some(concept) =>
        val query = Uri.Query(
          "c" -> Main.prefixes.getCurie(concept.getURI).orElse(concept.getURI),
          "pageSize" -> "1000000")
        for {
          response <- Http().singleRequest(HttpRequest(uri = s"$endpoint/statements/?$query"))
          responseString <- Unmarshal(response).to[String]
        } yield beaconResponseToModel(responseString.parseJson.convertTo[List[BeaconStatement]].filter(matches(_, s, p, o)), modelURI(s, p, o))
    }
  }

  private def port = if (Main.port == 80) "" else s":${Main.port}"
  private val server: String = s"http://${Main.hostname}$port"
  private val encodedEndpoint = URLEncoder.encode(endpoint, "utf-8")

  private def modelURI(s: Option[Resource], p: Option[Resource], o: Option[Resource]): Resource = {
    var params = Map.empty[String, String]
    s.foreach(params += "s" -> _.getURI)
    p.foreach(params += "p" -> _.getURI)
    o.foreach(params += "o" -> _.getURI)

    ResourceFactory.createResource(s"$server/beacon/$encodedEndpoint/pattern?${Uri.Query(params)}")
  }

  private def matches(bs: BeaconStatement, s: Option[Resource], p: Option[Resource], o: Option[Resource]): Boolean = {
    val sMatch = s.map(_.getURI == Main.prefixes.getIri(bs.subject.id).orElse(bs.subject.id)).getOrElse(true)
    val pMatch = p.map(_.getURI == Main.prefixes.getIri(bs.predicate.id).orElse(bs.predicate.id)).getOrElse(true)
    val oMatch = o.map(_.getURI == Main.prefixes.getIri(bs.`object`.id).orElse(bs.`object`.id)).getOrElse(true)
    sMatch && pMatch && oMatch
  }

  private def beaconResponseToModel(statements: List[BeaconStatement], modelURI: Resource): Model = {
    val model = ModelFactory.createDefaultModel()
    statements.map(beaconStatementToTriple).foreach(model.add)
    val modelSize = model.size.toInt
    val dataset = ResourceFactory.createResource(endpoint)
    model.add(ResourceFactory.createStatement(dataset, VOID.subset, modelURI))
    val form = ResourceFactory.createResource()
    model.add(ResourceFactory.createStatement(dataset, ResourceFactory.createProperty(s"$Hydra#search"), form))
    model.add(ResourceFactory.createStatement(form, ResourceFactory.createProperty(s"$Hydra#template"), ResourceFactory.createPlainLiteral(s"$server/beacon/$encodedEndpoint/pattern{?s,p,o}")))
    val subj = ResourceFactory.createResource()
    val pred = ResourceFactory.createResource()
    val obj = ResourceFactory.createResource()
    model.add(ResourceFactory.createStatement(form, ResourceFactory.createProperty(s"$Hydra#mapping"), subj))
    model.add(ResourceFactory.createStatement(form, ResourceFactory.createProperty(s"$Hydra#mapping"), pred))
    model.add(ResourceFactory.createStatement(form, ResourceFactory.createProperty(s"$Hydra#mapping"), obj))
    model.add(ResourceFactory.createStatement(subj, ResourceFactory.createProperty(s"$Hydra#variable"), ResourceFactory.createPlainLiteral("s")))
    model.add(ResourceFactory.createStatement(subj, ResourceFactory.createProperty(s"$Hydra#property"), RDF.subject))
    model.add(ResourceFactory.createStatement(pred, ResourceFactory.createProperty(s"$Hydra#variable"), ResourceFactory.createPlainLiteral("p")))
    model.add(ResourceFactory.createStatement(pred, ResourceFactory.createProperty(s"$Hydra#property"), RDF.predicate))
    model.add(ResourceFactory.createStatement(obj, ResourceFactory.createProperty(s"$Hydra#variable"), ResourceFactory.createPlainLiteral("o")))
    model.add(ResourceFactory.createStatement(obj, ResourceFactory.createProperty(s"$Hydra#property"), RDF.`object`))
    model.add(ResourceFactory.createStatement(modelURI, VOID.triples, ResourceFactory.createTypedLiteral(modelSize)))
    model
  }

  private def beaconStatementToTriple(bs: BeaconStatement): Statement = {
    def toResource(id: String): Resource = ResourceFactory.createResource(id)
    ResourceFactory.createStatement(
      toResource(Main.prefixes.getIri(bs.subject.id).orElse(bs.subject.id)),
      ResourceFactory.createProperty(toResource(Main.prefixes.getIri(bs.predicate.id).orElse(bs.predicate.id)).getURI),
      toResource(Main.prefixes.getIri(bs.`object`.id).orElse(bs.`object`.id)))
  }

}

object Beacon {

  val `text/turtle` = MediaType.customWithFixedCharset("text", "turtle", HttpCharsets.`UTF-8`, "ttl" :: Nil, Map.empty, false)

  implicit val modelMarshaller: ToEntityMarshaller[Model] = Marshaller.stringMarshaller(`text/turtle`).compose { model =>
    val writer = new StringWriter()
    RDFDataMgr.write(writer, model, RDFFormat.TURTLE)
    writer.toString
  }

  implicit val resourceUnmarshaller = Unmarshaller.strict(ResourceFactory.createResource)

}