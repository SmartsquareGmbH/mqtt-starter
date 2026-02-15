import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dokka)
    alias(libs.plugins.dokka.javadoc)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.test.logger)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.versions)
}

group = "de.smartsquare"
version = System.getenv("GITHUB_VERSION") ?: "1.0.0-SNAPSHOT"
description = "Spring Boot Starter wrapping the hivemq mqtt client."

repositories {
    mavenCentral()
}

dependencies {

    compileOnly(libs.kotlinx.coroutines.core)

    compileOnly(platform(libs.spring.boot.dependencies))
    compileOnly(libs.spring.boot)
    compileOnly(libs.spring.boot.starter.validation)
    compileOnly(libs.spring.boot.starter.actuator)

    compileOnly(libs.jackson.databind)
    compileOnly(libs.gson)

    api(libs.hivemq.mqtt.client)

    testImplementation(libs.kotlinx.coroutines.core)

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.json)
    testImplementation(libs.spring.boot)
    testImplementation(libs.spring.boot.autoconfigure)
    testImplementation(libs.spring.boot.starter.validation)
    testImplementation(libs.spring.boot.starter.actuator)

    testImplementation(libs.jackson.databind)
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(libs.gson)

    testImplementation(libs.kluent)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.awaitility.kotlin)
    testImplementation(libs.testcontainers.junit.jupiter)

    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        allWarningsAsErrors = true
        javaParameters = true
    }

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true
    }
}

detekt {
    config.setFrom("${project.rootDir}/detekt.yml")

    buildUponDefaultConfig = true
}

dokka {
    dokkaSourceSets.configureEach {
        externalDocumentationLinks.register("hivemq-mqtt-client") {
            url("https://javadoc.io/doc/com.hivemq/hivemq-mqtt-client/${libs.versions.hivemq.mqtt.client.get()}")
            packageListUrl(
                "https://javadoc.io/doc/com.hivemq/hivemq-mqtt-client/${libs.versions.hivemq.mqtt.client.get()}/element-list",
            )
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        excludeEngines("junit-vintage")
    }
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return !isStable
}

mavenPublishing {
    configure(KotlinJvm(JavadocJar.Dokka(tasks.dokkaGeneratePublicationJavadoc.name)))

    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    pom {
        name = "Mqtt-Starter"
        description = "Spring Boot Starter wrapping the hivemq mqtt client."
        url = "https://github.com/SmartsquareGmbH/mqtt-starter"
        inceptionYear = "2021"

        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/license/mit"
                distribution = "https://opensource.org/license/mit"
            }
        }
        developers {
            developer {
                id = "deen13"
                name = "Dennis Dierkes"
                email = "dierkes@smartsquare.de"
                organization = "Smartsquare GmbH"
                organizationUrl = "https://github.com/SmartsquareGmbH"
            }
            developer {
                id = "rubengees"
                name = "Ruben Gees"
                email = "gees@smartsquare.de"
                organization = "Smartsquare GmbH"
                organizationUrl = "https://github.com/SmartsquareGmbH"
            }
        }
        scm {
            url = "https://github.com/SmartsquareGmbH/mqtt-starter"
            connection = "scm:git:https://github.com/SmartsquareGmbH/mqtt-starter.git"
            developerConnection = "scm:git:ssh://github.com/SmartsquareGmbH/mqtt-starter.git"
        }
        organization {
            name = "Smartsquare GmbH"
            url = "https://github.com/SmartsquareGmbH"
        }
        issueManagement {
            system = "GitHub"
            url = "https://github.com/SmartsquareGmbH/mqtt-starter/issues"
        }
    }
}

tasks.withType<Wrapper> {
    gradleVersion = libs.versions.gradle.get()
}
