import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("signing")
    id("maven-publish")
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.kotlin.plugin.spring") version "1.9.22"
    id("org.jetbrains.dokka") version "1.9.10"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
    id("org.jmailen.kotlinter") version "4.2.0"
    id("com.adarshr.test-logger") version "4.0.0"
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
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

    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.2"))
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    api("com.hivemq:hivemq-mqtt-client:1.3.3")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.amshove.kluent:kluent:1.73")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.awaitility:awaitility-kotlin")
    testImplementation("org.testcontainers:junit-jupiter")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withJavadocJar()
    withSourcesJar()
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        allWarningsAsErrors = true
    }
}

detekt {
    config.setFrom("${project.rootDir}/detekt.yml")

    buildUponDefaultConfig = true
}

tasks.named<Jar>("javadocJar") {
    from(tasks.named("dokkaJavadoc"))
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        externalDocumentationLink("https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-databind/latest/")
        externalDocumentationLink("https://javadoc.io/doc/com.hivemq/hivemq-mqtt-client/latest/")
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "mqtt-starter"

            from(components["java"])

            pom {
                name = "Mqtt-Starter"
                description = "Spring Boot Starter wrapping the hivemq mqtt client."
                url = "https://github.com/SmartsquareGmbH/mqtt-starter"

                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }
                developers {
                    developer {
                        id = "deen13"
                        name = "Dennis Dierkes"
                        email = "dierkes@smartsquare.de"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/SmartsquareGmbH/mqtt-starter.git"
                    developerConnection = "scm:git:ssh://github.com/SmartsquareGmbH/mqtt-starter.git"
                    url = "https://github.com/SmartsquareGmbH/mqtt-starter"
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
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username = project.findProperty("gpr.ossrhUser")?.toString() ?: System.getenv("OSSRHUSER")
            password = project.findProperty("gpr.ossrhPassword")?.toString() ?: System.getenv("OSSRHPASSWORD")
        }
    }
}

signing {
    if (!version.toString().endsWith("SNAPSHOT")) {
        val signingKey = findProperty("signingKey")?.toString() ?: System.getenv("GPG_PRIVATE_KEY")
        val signingPassword = findProperty("signingPassword")?.toString() ?: System.getenv("GPG_PASSPHRASE")
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "8.5"
}
