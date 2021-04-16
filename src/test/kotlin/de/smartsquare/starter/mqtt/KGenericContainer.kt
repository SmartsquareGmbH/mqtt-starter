package de.smartsquare.starter.mqtt

import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

class KGenericContainer(imageName: DockerImageName) : GenericContainer<KGenericContainer>(imageName)
