package org.apache.olingo.example.metadata

import java.util

import org.apache.olingo.commons.api.edm.{EdmPrimitiveTypeKind, FullQualifiedName}
import org.apache.olingo.commons.api.edm.provider._

class DummyEdmProvider(namespace: String, container_name: String, container: FullQualifiedName, et_dummy_name: String, et_dummy_fqn: FullQualifiedName, es_dummy_name: String) extends CsdlAbstractEdmProvider {

  override def getEntityType(entityTypeName: FullQualifiedName): CsdlEntityType = {
    if (entityTypeName.equals(et_dummy_fqn)) {
      val id = new CsdlProperty().setName("ID").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName).setNullable(false)
      val name = new CsdlProperty().setName("NAME").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName)
      val url = new CsdlProperty().setName("URL").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName)

      val propertyRef = new CsdlPropertyRef()
      propertyRef.setName("ID")

      val entityType = new CsdlEntityType()
      entityType.setName(et_dummy_name)
      entityType.setProperties(java.util.Arrays.asList(id, name, url))
      entityType.setKey(java.util.Collections.singletonList(propertyRef))

      return entityType
    }
    null
  }

  override def getEntitySet(entityContainer: FullQualifiedName, entitySetName: String): CsdlEntitySet = {
    if (entityContainer.equals(container)) {
      if (entitySetName.equals(es_dummy_name)) {
        val entitySet = new CsdlEntitySet()
        entitySet.setName(es_dummy_name)
        entitySet.setType(et_dummy_fqn)

        return entitySet
      }
    }
    null
  }

  override def getEntityContainer(): CsdlEntityContainer = {
    val entitySets = new util.ArrayList[CsdlEntitySet]()
    entitySets.add(getEntitySet(container, es_dummy_name))

    val entityContainer = new CsdlEntityContainer()
    entityContainer.setName(container_name)
    entityContainer.setEntitySets(entitySets)

    entityContainer
  }

  override def getSchemas(): util.List[CsdlSchema] = {
    val schema = new CsdlSchema()
    schema.setNamespace(namespace)

    val entityTypes = new util.ArrayList[CsdlEntityType]()
    entityTypes.add(getEntityType(et_dummy_fqn))
    schema.setEntityTypes(entityTypes)

    schema.setEntityContainer(getEntityContainer())

    val schemas: util.List[CsdlSchema] = new util.ArrayList[CsdlSchema]()
    schemas.add(schema)

    schemas
  }

  override def getEntityContainerInfo(entityContainerName: FullQualifiedName): CsdlEntityContainerInfo = {
    if (entityContainerName == null || entityContainerName.equals(container)) {
      val entityContainerInfo = new CsdlEntityContainerInfo()
      entityContainerInfo.setContainerName(container)
      return entityContainerInfo
    }
    null
  }
}

object DummyEdmProvider {
  private val namespace = "OData.Demo"

  private val container_name = "Container"
  private val container = new FullQualifiedName(namespace, container_name)

  private val et_dummy_name = "Dummy"
  private val et_dummy_fqn = new FullQualifiedName(namespace, et_dummy_name)

  private val es_dummy_name = "Dummies"

  def apply(): DummyEdmProvider = new DummyEdmProvider(namespace, container_name, container, et_dummy_name, et_dummy_fqn, es_dummy_name)
}