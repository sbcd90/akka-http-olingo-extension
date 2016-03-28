package org.apache.olingo.example

import java.util

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import org.apache.olingo.example.metadata.DummyEdmProvider
import org.apache.olingo.example.processors.DummyCollectionProcessor
import org.apache.olingo.server.api.OData
import org.apache.olingo.server.api.edmx.EdmxReference
import org.apache.olingo.server.core.OdataAkkaHttpHandlerImpl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MainApplication extends App {
  implicit val system = ActorSystem("akka-http-olingo-integration")
  implicit val fm = ActorMaterializer()

  val serverBinding = Http().bindAndHandleAsync(asyncHandler, interface = "localhost", port = 8080)

  def asyncHandler(request: HttpRequest): Future[HttpResponse] = {
    request match {
      case HttpRequest(GET, _, _, _, _) => {
        Future[HttpResponse] {
          val oData = OData.newInstance()

          val edm = oData.createServiceMetadata(DummyEdmProvider(), new util.ArrayList[EdmxReference]())
          val handler = new OdataAkkaHttpHandlerImpl(oData, edm, fm)

          handler.register(new DummyCollectionProcessor())
          handler.process(request)
        }
      }
    }
  }
}