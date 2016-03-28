package org.apache.olingo.server.core

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.StreamConverters
import org.apache.commons.io.IOUtils
import org.apache.olingo.commons.api.http.HttpMethod
import org.apache.olingo.server.api.processor.Processor
import org.apache.olingo.server.api.{OData, ODataRequest, ODataResponse, ServiceMetadata}
import org.apache.olingo.server.core.debug.ServerCoreDebugger

import scala.concurrent.duration.FiniteDuration

class OdataAkkaHttpHandlerImpl(val oData: OData, val serviceMetadata: ServiceMetadata, val fm: ActorMaterializer) extends ODataHttpHandlerImpl(oData, serviceMetadata) {
  private val debugger = new ServerCoreDebugger(oData)
  private val handler = new ODataHandler(oData, serviceMetadata, debugger)

  def process(httpRequest: HttpRequest): HttpResponse = {
    val odRequest = new ODataRequest()

    var exception: Exception = null

    var odResponse: ODataResponse = null

    fillODataRequest(odRequest, httpRequest)
    odResponse = handler.process(odRequest)
    getHttpResponse(odResponse)
  }

  override def register(processor: Processor): Unit = {
    handler.register(processor)
  }

  private def findHeader(httpRequest: HttpRequest, headerName: String): String = {
    httpRequest.headers.foreach(httpHeader => {
      if (httpHeader.name().equals(headerName))
        return httpHeader.value()
    })
    null
  }

  private def getMethod(httpRequest: HttpRequest): HttpMethod = {
    val httpRequestMethod = HttpMethod.valueOf(httpRequest.method.value)

    if (httpRequestMethod == HttpMethod.POST) {
      val xHttpMethod = findHeader(httpRequest, org.apache.olingo.commons.api.http.HttpHeader.X_HTTP_METHOD)
      val xHttpMethodOverride = findHeader(httpRequest, org.apache.olingo.commons.api.http.HttpHeader.X_HTTP_METHOD_OVERRIDE)

      if (xHttpMethod == null && xHttpMethodOverride == null) {
        httpRequestMethod
      }
      else if (xHttpMethod == null) {
        HttpMethod.valueOf(xHttpMethodOverride)
      }
      else if (xHttpMethodOverride == null) {
        HttpMethod.valueOf(xHttpMethod)
      }
      else {
        if (!xHttpMethod.equalsIgnoreCase(xHttpMethodOverride)) {
          throw new ODataHandlerException("Ambiguous X-HTTP-Methods", ODataHandlerException.MessageKeys.AMBIGUOUS_XHTTP_METHOD, xHttpMethod, xHttpMethodOverride)
        }
        HttpMethod.valueOf(xHttpMethod)
      }
    }
    else {
      httpRequestMethod
    }
  }

  private def getHeaders(odRequest: ODataRequest, httpRequest: HttpRequest): Unit = {
    httpRequest.headers.foreach(httpHeader => {
      val name = httpHeader.name()

      val values: java.util.List[String] = new java.util.ArrayList[String]()
      httpRequest.headers.foreach(httpHeader => {
        if (httpHeader.name().equals(name))
          values.add(httpHeader.value())
      })

      odRequest.addHeader(name, values)
    })
  }

  private def getUriInformation(odRequest: ODataRequest, httpRequest: HttpRequest): Unit = {
    val rawRequestUri = httpRequest.uri.toString()

    //Todo handle any prefix url before actual Entity Name. see ODataHttpHandlerImpl.fillUriInformation
    val rawODataPath = httpRequest.uri.path.toString()

    //Todo handle split. see ODataHttpHandlerImpl.fillUriInformation
    var rawServiceResolutionUri: String = null

    val rawBaseUri = rawRequestUri.substring(0, rawRequestUri.length - rawODataPath.length)

    if (httpRequest.uri.queryString().isEmpty)
      odRequest.setRawQueryPath("")
    else
      odRequest.setRawQueryPath(httpRequest.uri.queryString().get)

    if (httpRequest.uri.queryString().isEmpty)
      odRequest.setRawRequestUri(rawRequestUri + "")
    else
      odRequest.setRawRequestUri(rawRequestUri + "?" + httpRequest.uri.queryString().get)
    odRequest.setRawODataPath(rawODataPath)
    odRequest.setRawBaseUri(rawBaseUri)
    odRequest.setRawServiceResolutionUri(rawServiceResolutionUri)
  }

  private def statusCodeConverter(statusCode: Int): StatusCode = statusCode match {
    case 200 => StatusCodes.OK
    case 400 => StatusCodes.BadRequest
    case 500 => StatusCodes.InternalServerError
    case _ => StatusCodes.BadRequest
  }

  private def getHttpResponse(odResponse: ODataResponse): HttpResponse = {
    val statusCode = statusCodeConverter(odResponse.getStatusCode)

    val entity = IOUtils.toString(odResponse.getContent, "UTF-8")

    var headers: List[HttpHeader] = List[HttpHeader]()

    import scala.collection.JavaConversions._
    for ((name, values) <- odResponse.getAllHeaders.toMap) {

      values.foreach(value => {
        if (!name.equals("Content-Type")) {
          headers ++= List(RawHeader(name, value))
        }
      })
    }

    HttpResponse(status = statusCode, headers = headers, entity = HttpEntity(akka.http.scaladsl.model.ContentType(MediaTypes.`application/json`), entity))
  }

  private def fillODataRequest(odRequest: ODataRequest, httpRequest: HttpRequest): ODataRequest = {
    odRequest.setBody(httpRequest.entity.getDataBytes().runWith(StreamConverters.asInputStream(FiniteDuration(3, TimeUnit.SECONDS)), fm))
    odRequest.setProtocol(httpRequest.protocol.value)
    odRequest.setMethod(getMethod(httpRequest))
    getHeaders(odRequest, httpRequest)
    getUriInformation(odRequest, httpRequest)
    odRequest
  }
}