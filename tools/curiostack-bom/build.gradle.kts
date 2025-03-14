/*
 * MIT License
 *
 * Copyright (c) 2018 Choko (choko@curioswitch.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import java.net.URI

plugins {
    `java-platform`
    `maven-publish`
    id("com.google.cloud.artifactregistry.gradle-plugin")
    id("com.github.ben-manes.versions")
}

javaPlatform {
    allowDependencies()
}

repositories {
        gradlePluginPortal {
                content {
                        excludeModuleByRegex("org\\.curioswitch\\..*", "(?!protobuf-jackson)")
                }
        }
        maven {
                url = URI(rootProject.findProperty("org.curioswitch.curiostack.repo_uri") as String)
        }
}

data class DependencySet(val group: String, val version: String, val modules: List<String>)

val GRPC_VERSION = "1.65.0"
val PROTOBUF_VERSION = "3.25.3"
// Newer versions of protobuf are currently not compatible with currently available newest version of grpc-java library
// https://github.com/grpc/grpc-java/issues/11015
// https://github.com/protocolbuffers/protobuf/issues/17247

val DEPENDENCY_BOMS = listOf(
        "com.fasterxml.jackson:jackson-bom:2.17.2",
        "com.google.cloud:google-cloud-bom:0.224.0",
        "com.google.api-client:google-api-client-bom:2.6.0",
        "com.google.guava:guava-bom:33.2.1-jre",
        "com.google.http-client:google-http-client-bom:1.44.2",
        "com.google.protobuf:protobuf-bom:${PROTOBUF_VERSION}",
        "com.linecorp.armeria:armeria-bom:1.29.1",
        "io.dropwizard.metrics:metrics-bom:4.2.26",
        "io.grpc:grpc-bom:${GRPC_VERSION}",
        "io.micrometer:micrometer-bom:1.13.2",
        "io.zipkin.brave:brave-bom:5.18.1", // 6.0.3
        "io.netty:netty-bom:4.1.111.Final",
        "org.apache.beam:beam-sdks-java-bom:2.57.0",
        "org.apache.logging.log4j:log4j-bom:2.23.1",
        "org.junit:junit-bom:5.10.3",
)

val DEPENDENCY_SETS = listOf(
        DependencySet(
                "com.auth0",
                "4.4.0",
                listOf("java-jwt")
        ),
        DependencySet(
                "com.google.auth",
                "1.23.0",
                listOf("google-auth-library-oauth2-http")
        ),
        // Not updating com.google.auto.factory:auto-factory
        // because 1.1.0 breaks org.curioswitch.common.server.framework.auth.jwt.JwtVerifier.Factory
        DependencySet(
                "com.google.auto.factory",
                "1.0.1",
                listOf("auto-factory")
        ),
        DependencySet(
                "com.google.auto.service",
                "1.1.1",
                listOf("auto-service")
        ),
        DependencySet(
                "com.google.auto.value",
                "1.11.0",
                listOf("auto-value", "auto-value-annotations")
        ),
        DependencySet(
                "com.github.ben-manes.caffeine",
                "3.1.8",
                listOf("caffeine")
        ),
        DependencySet(
                "com.google.auto",
                "1.2.2",
                listOf("auto-common")
        ),
        DependencySet(
                "com.google.code.findbugs",
                "3.0.2", // Last update 2017-03-31
                listOf("jsr305")
        ),
        DependencySet(
                "com.google.cloud.sql",
                "1.19.0",
                listOf("mysql-socket-factory-connector-j-8")
        ),
        DependencySet(
                "com.google.dagger",
                "2.51.1",
                listOf("dagger", "dagger-compiler", "dagger-producers")
        ),
        DependencySet(
                "com.google.errorprone",
                "2.27.1",
                listOf("error_prone_annotations", "error_prone_core")
        ),
        DependencySet(
                "com.google.firebase",
                "9.3.0",
                listOf("firebase-admin")
        ),
        DependencySet(
                "com.google.protobuf",
                PROTOBUF_VERSION,
                listOf("protoc")
        ),
        DependencySet(
                "com.spotify",
                "4.3.3",
                listOf("futures-extra")
        ),
        DependencySet(
                "com.squareup.retrofit2",
                "2.11.0",
                listOf(
                        "adapter-guava",
                        "adapter-java8",
                        "converter-guava",
                        "converter-jackson",
                        "converter-java8",
                        "retrofit"
                )
        ),
        DependencySet(
                "com.typesafe",
                "1.4.3",
                listOf("config")
        ),
        DependencySet(
                "com.zaxxer",
                "5.1.0",
                listOf("HikariCP")
        ),
        DependencySet(
                "info.solidsoft.mockito",
                "2.5.0", // Last update 2018-10-19
                listOf("mockito-java8")
        ),
        // grpc-bom can only be applied to Java projects because it does not export Gradle metadata. For
        // non-Java projects compiling gRPC stubs, they will only use these artifacts so we go ahead and manage
        // then in curiostack-bom as well.
        DependencySet(
                "io.grpc",
                GRPC_VERSION,
                listOf("grpc-core", "grpc-protobuf", "grpc-stub")
        ),
        DependencySet(
                "io.lettuce",
                "6.3.2.RELEASE",
                listOf("lettuce-core")
        ),
        DependencySet(
                "io.netty",
                "2.0.65.Final",
                listOf("netty-tcnative-boringssl-static")
        ),
        DependencySet(
                "io.prometheus",
                "0.16.0", // Last updated 2022-06-15
                listOf("simpleclient", "simpleclient_common", "simpleclient_hotspot", "simpleclient_log4j2")
        ),
        DependencySet(
                "io.zipkin.gcp",
                "1.1.1", // 2.2.3
                listOf("brave-propagation-stackdriver", "zipkin-translation-stackdriver")
        ),
        DependencySet(
                "jakarta.annotation",
                "3.0.0",
                listOf("jakarta.annotation-api")
        ),
        DependencySet(
                "jakarta.inject",
                "2.0.1", // Last update 2021-10-16
                listOf("jakarta.inject-api")
        ),
        DependencySet(
                "junit",
                "4.13.2", // org.junit.jupiter:junit-jupiter-api:5.10.2
                listOf("junit")
        ),
        DependencySet(
                "org.assertj",
                "3.26.0",
                listOf("assertj-core", "assertj-guava")
        ),
        DependencySet(
                "org.awaitility",
                "4.2.1",
                listOf("awaitility")
        ),
        DependencySet(
                "org.bouncycastle",
                "1.78.1",
                listOf("bcpkix-jdk18on", "bcprov-jdk18on")
        ),
        DependencySet(
                "org.checkerframework",
                "3.45.0",
                listOf("checker-qual")
        ),
        DependencySet(
                "org.eclipse.jgit",
                "6.10.0.202406032230-r",
                listOf("org.eclipse.jgit", "org.eclipse.jgit.ssh.apache", "org.eclipse.jgit.ssh.jsch")
        ),
        DependencySet(
                "org.immutables",
                "2.10.1",
                listOf("builder", "value", "value-annotations")
        ),
        DependencySet(
                "org.jctools",
                "4.0.5",
                listOf("jctools-core")
        ),
        DependencySet(
                "org.jooq",
                "3.19.10",
                listOf("jooq", "jooq-codegen", "jooq-meta")
        ),
        DependencySet(
                "org.mockito",
                "4.11.0", // 5.12.0
                listOf("mockito-core", "mockito-junit-jupiter")
        ),
        DependencySet(
                "org.slf4j",
                "2.0.13",
                listOf("jul-to-slf4j", "slf4j-api", "slf4j-simple")
        ),
        DependencySet(
                "org.simpleflatmapper",
                "8.2.3", // 9.0.0
                listOf(
                        "sfm-converter", "sfm-jdbc", "sfm-jooq", "sfm-map", "sfm-reflect", "sfm-util"
                )
        ),
        DependencySet(
                "org.flywaydb",
                "6.5.7",
                listOf("flyway-core", "flyway-gradle-plugin")
        ),
)

val DEPENDENCIES = listOf(
        "gradle.plugin.com.google.cloud.artifactregistry:artifactregistry-gradle-plugin:2.2.1",
        "com.bmuschko:gradle-docker-plugin:9.4.0",
        "com.diffplug.spotless:spotless-plugin-gradle:6.25.0",
        "com.github.ben-manes:gradle-versions-plugin:0.51.0",
        "com.google.code.gson:gson:2.11.0",
        "com.google.gradle:osdetector-gradle-plugin:1.7.3",
        "com.gorylenko.gradle-git-properties:gradle-git-properties:2.4.2",
        "com.gradle:gradle-enterprise-gradle-plugin:3.17.5",
        "com.hubspot.jinjava:jinjava:2.7.2",
        "com.uber.nullaway:nullaway:0.11.0",
        "com.google.cloud.tools:jib-gradle-plugin:3.4.3",
        "gradle.plugin.nl.javadude.gradle.plugins:license-gradle-plugin:0.14.0", // Last update 2017-04-25
        "it.unimi.dsi:fastutil:8.5.13",
        "jakarta.activation:jakarta.activation-api:2.1.3",
        "jakarta.annotation:jakarta.annotation-api:3.0.0",
        "javax.annotation:javax.annotation-api:1.3.2", // Old version still needed by some auto generated code
        "me.champeau.gradle:jmh-gradle-plugin:0.5.3", // Last update 2021-02-09
        "com.mysql:mysql-connector-j:8.4.0",
        "net.ltgt.gradle:gradle-errorprone-plugin:4.0.1",
        "net.ltgt.gradle:gradle-nullaway-plugin:2.0.0",
        "nu.studer:gradle-jooq-plugin:7.1.1", // 9.0 - 8+ requires java and gradle upgrade
        "org.curioswitch.curiostack:protobuf-jackson:2.5.0",
        "org.ow2.asm:asm:9.7",
        "org.jsoup:jsoup:1.17.2",
        "jakarta.xml.bind:jakarta.xml.bind-api:4.0.2",
)

val bomProject = project
rootProject.allprojects {
    if (path.startsWith(":common:")) {
        bomProject.evaluationDependsOn(path)
    }
}

dependencies {
    for (bom in DEPENDENCY_BOMS) {
        api(enforcedPlatform(bom))
    }
    constraints {
        rootProject.allprojects {
            if (path.startsWith(":common:")) {
                plugins.withId("maven-publish") {
                    api("${group}:${base.archivesBaseName}:${version}")
                }
            }
        }

        for (set in DEPENDENCY_SETS) {
            for (module in set.modules) {
                api("${set.group}:${module}:${set.version}")
            }
        }
        for (dependency in DEPENDENCIES) {
            api(dependency)
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components.getByName("javaPlatform"))

            pom {
                name.set("Curiostack Bill-of-Materials")
                description.set("BOM specifying versions for all standard Curiostack dependencies.")
                url.set(
                        "https://github.com/infostellarinc/curiostack/tree/master/tools/" +
                                "curiostack-bom"
                )
                setPackaging("pom")
            }
        }
    }
    repositories {
        maven {
            url = URI(rootProject.findProperty("org.curioswitch.curiostack.repo_uri") as String)
        }
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version) || version.endsWith("-jre")
    return isStable.not()
}

tasks {
    named<DependencyUpdatesTask>("dependencyUpdates") {
        revision = "release"
        checkConstraints = true

        rejectVersionIf {
            isNonStable(candidate.version)
        }
    }

    withType<GenerateModuleMetadata> {
        suppressedValidationErrors.add("enforced-platform")
    }
}
