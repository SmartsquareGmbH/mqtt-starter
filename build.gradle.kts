import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.20"
    id("org.jetbrains.kotlin.plugin.spring") version "2.0.20"
    id("dev.adamko.dokkatoo-html") version "2.3.1"
    id("dev.adamko.dokkatoo-javadoc") version "2.3.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("org.jmailen.kotlinter") version "4.4.1"
    id("com.adarshr.test-logger") version "4.0.0"
    id("com.vanniktech.maven.publish") version "0.29.0"
    id("com.github.ben-manes.versions") version "0.51.0"
}

group = "de.smartsquare"
version = System.getenv("GITHUB_VERSION") ?: "1.0.0-SNAPSHOT"
description = "Spring Boot Starter wrapping the hivemq mqtt client."

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
    implementation("org.springframework.boot:spring-boot")
    compileOnly("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    api("com.hivemq:hivemq-mqtt-client:1.3.3")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation("org.amshove.kluent:kluent:1.73")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.awaitility:awaitility-kotlin")
    testImplementation("org.testcontainers:junit-jupiter")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        allWarningsAsErrors = true
        javaParameters = true
    }
}

detekt {
    config.setFrom("${project.rootDir}/detekt.yml")

    buildUponDefaultConfig = true
}

dokkatoo {
    val jacksonVersion = resolveVersion("com.fasterxml.jackson.core:jackson-core")
    val mqttClientVersion = resolveVersion("com.hivemq:hivemq-mqtt-client")

    dokkatooSourceSets.configureEach {
        externalDocumentationLinks.create("jackson") {
            url("https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-databind/$jacksonVersion/")
        }

        externalDocumentationLinks.create("hivemq-mqtt-client") {
            url("https://javadoc.io/doc/com.hivemq/hivemq-mqtt-client/$mqttClientVersion/")
        }
    }
}

fun resolveVersion(dependency: String): String {
    return project.configurations.getByName("runtimeClasspath").resolvedConfiguration.resolvedArtifacts
        .find { it.moduleVersion.id.module.toString() == dependency }
        ?.moduleVersion?.id?.version
        ?: "latest"
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
    configure(KotlinJvm(JavadocJar.Dokka("dokkatooGeneratePublicationJavadoc")))

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
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
            }
            developer {
                id = "rubengees"
                name = "Ruben Gees"
                email = "gees@smartsquare.de"
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
    gradleVersion = "8.10"
}
