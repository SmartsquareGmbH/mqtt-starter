package de.smartsquare.starter.mqtt

import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.ObjectProvider

class TestObjectProvider<T : Any>(private val data: T?) : ObjectProvider<T> {
    override fun getObject(vararg args: Any?): T = data ?: throw NoSuchBeanDefinitionException("No bean found")
    override fun getObject(): T = data ?: throw NoSuchBeanDefinitionException("No bean found")
    override fun getIfAvailable(): T? = data
    override fun getIfUnique(): T = getObject()
}
