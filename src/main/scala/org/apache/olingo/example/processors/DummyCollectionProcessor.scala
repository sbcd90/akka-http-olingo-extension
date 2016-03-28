package org.apache.olingo.example.processors

import java.net.URI

import org.apache.olingo.commons.api.data._
import org.apache.olingo.commons.api.format.ContentType
import org.apache.olingo.commons.api.http.{HttpHeader, HttpStatusCode}
import org.apache.olingo.server.api.{OData, ODataRequest, ODataResponse, ServiceMetadata}
import org.apache.olingo.server.api.processor.EntityCollectionProcessor
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions
import org.apache.olingo.server.api.uri.{UriInfo, UriResourceEntitySet}

class DummyCollectionProcessor extends EntityCollectionProcessor {
  private var oData: OData = null

  private var serviceMetadata: ServiceMetadata = null

  def init(oData: OData, serviceMetadata: ServiceMetadata): Unit = {
    this.oData = oData
    this.serviceMetadata = serviceMetadata
  }

  def readEntityCollection(oDataRequest: ODataRequest, oDataResponse: ODataResponse, uriInfo: UriInfo, contentType: ContentType): Unit = {
    val uriResources = uriInfo.getUriResourceParts

    val uriResourceEntitySet = uriResources.get(0).asInstanceOf[UriResourceEntitySet]
    val edmEntitySet = uriResourceEntitySet.getEntitySet

    val dummyCollection = new EntityCollection()
    val entity = new Entity()
                      .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, "nexus-milestones"))
                      .addProperty(new Property(null, "NAME", ValueType.PRIMITIVE, "nexus-milestones"))
                      .addProperty(new Property(null, "URL", ValueType.PRIMITIVE, "http://repo2.maven.apache.org"))

    entity.setId(createId("Dummies", "nexus-milestones"))
    dummyCollection.getEntities.add(entity)

    val serializer = oData.createSerializer(contentType)
    val edmEntityType = edmEntitySet.getEntityType

    val id = oDataRequest.getRawBaseUri + "/" + edmEntitySet.getName
    val contextURL = ContextURL.`with`().entitySet(edmEntitySet).build()
    val opts = EntityCollectionSerializerOptions.`with`().id(id).contextURL(contextURL).build()
    val serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, dummyCollection, opts)
    val serializedContent = serializerResult.getContent

    oDataResponse.setContent(serializedContent)
    oDataResponse.setStatusCode(HttpStatusCode.OK.getStatusCode)
    oDataResponse.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString)
  }

  def createId(entitySetName: String, key: Object): URI = {
    new URI(entitySetName + "('" + String.valueOf(key) + "')")
  }
}