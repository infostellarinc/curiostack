/*
 * MIT License
 *
 * Copyright (c) 2017 Choko (choko@curioswitch.org)
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

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.net.URI

plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.google.cloud.artifactregistry.gradle-plugin")
}

dependencies {
    implementation(project(":common:google-cloud:cloud-storage"))
    implementation(project(":tools:gradle-plugins:gradle-golang-plugin"))
    implementation(project(":tools:gradle-plugins:gradle-protobuf-plugin"))
    implementation(project(":tools:gradle-plugins:gradle-helpers"))
    implementation(project(":tools:gradle-plugins:gradle-release-plugin"))

    implementation("gradle.plugin.com.google.cloud.artifactregistry:artifactregistry-gradle-plugin")
    implementation("com.bmuschko:gradle-docker-plugin")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava")
    implementation("com.diffplug.spotless:spotless-plugin-gradle")
    implementation("com.github.ben-manes:gradle-versions-plugin")
    implementation("com.google.auth:google-auth-library-oauth2-http")
    implementation("com.google.cloud:google-cloud-kms")
    implementation("com.google.cloud.tools:jib-build-plan:0.4.0")
    implementation("com.google.guava:guava")
    implementation("com.gorylenko.gradle-git-properties:gradle-git-properties")
    implementation("com.hubspot.jinjava:jinjava")
    implementation("com.google.cloud.tools:jib-gradle-plugin")
    implementation("gradle.plugin.nl.javadude.gradle.plugins:license-gradle-plugin")
    implementation("me.champeau.gradle:jmh-gradle-plugin")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin")
    implementation("net.ltgt.gradle:gradle-nullaway-plugin")
    implementation("nu.studer:gradle-jooq-plugin")
    implementation("org.bouncycastle:bcpkix-jdk18on")
    implementation("org.eclipse.jgit:org.eclipse.jgit")
    implementation("org.jsoup:jsoup")
    implementation("org.flywaydb:flyway-gradle-plugin")

    // Prevent dependency hell for plugin users by specifying bom"d versions of grpc here
    runtimeOnly("io.grpc:grpc-core")
    runtimeOnly("io.grpc:grpc-netty-shaded")

    // Flyway plugin uses the gradle classpath, so adding this allows flyway to access cloud sql.
    runtimeOnly("com.google.cloud.sql:mysql-socket-factory-connector-j-8")
    runtimeOnly("com.mysql:mysql-connector-j")

    compileOnly(project(":common:curio-helpers"))

    annotationProcessor("org.immutables:value")
    compileOnly("org.immutables:value-annotations")

    testImplementation(project(":tools:gradle-plugins:gradle-test-helpers"))
}

gradlePlugin {
    plugins {
        register("buildCache") {
            id = "org.curioswitch.gradle-gcloud-build-cache-plugin"
            displayName = "Gradle Google Cloud Storage Build Cache Plugin"
            description = "Plugin for using GCP Cloud Storage as the backend for Gradle's build cache"
            implementationClass = "org.curioswitch.gradle.plugins.gcloud.GcloudBuildCachePlugin"
        }
        register("ci") {
            id = "org.curioswitch.gradle-curio-generic-ci-plugin"
            displayName = "Gradle Curio CI Plugin"
            description = "Plugin which adds a monorepo aware continuousBuild task for efficiently building on continuous integration"
            implementationClass = "org.curioswitch.gradle.plugins.ci.CurioGenericCiPlugin"
        }
        register("curiostack") {
            id = "org.curioswitch.gradle-curiostack-plugin"
            displayName = "Gradle Curiostack Plugin"
            description = "A highly opinionated plugin for setting up a gradle codebase " +
                    "using Curiostack conventions. Defines shared configuration and " +
                    "applies other useful plugins in an aim to be the only plugin in a " +
                    "root project."
            implementationClass = "org.curioswitch.gradle.plugins.curiostack.CuriostackPlugin"
        }
        register("database") {
            id = "org.curioswitch.gradle-curio-database-plugin"
            displayName = "Gradle Database Plugin"
            description = "Plugin to simplify defining database schema using flyway and other utilities for dealing with databases"
            implementationClass = "org.curioswitch.gradle.plugins.gcloud.CurioDatabasePlugin"
        }
        register("gcloud") {
            id = "org.curioswitch.gradle-gcloud-plugin"
            displayName = "Gradle Gcloud Plugin"
            description = "Plugin for interacting with the gcloud SDK and caching of tools using cloud storage"
            implementationClass = "org.curioswitch.gradle.plugins.gcloud.GcloudPlugin"
        }
        register("grpcApi") {
            id = "org.curioswitch.gradle-grpc-api-plugin"
            displayName = "Gradle gRPC API Plugin"
            description = "Plugin to reduce boilerplate for projects that define a gRPC API and adds support for gRPC-Web"
            implementationClass = "org.curioswitch.gradle.plugins.grpcapi.GrpcApiPlugin"
        }
        register("node") {
            id = "org.curioswitch.gradle-node-plugin"
            displayName = "Gradle Node Plugin"
            description = "Plugin for invoking NodeJS"
            implementationClass = "org.curioswitch.gradle.plugins.nodejs.NodePlugin"
        }
        register("server") {
            id = "org.curioswitch.gradle-curio-server-plugin"
            displayName = "Gradle Curio Server Plugin"
            description = "Plugin for building and deploying container images for Curiostack servers"
            implementationClass = "org.curioswitch.gradle.plugins.curioserver.CurioServerPlugin"
        }
        register("staticSite") {
            id = "org.curioswitch.gradle-static-site-plugin"
            displayName = "Gradle Static Site Plugin"
            description = "Plugin for deploying static sites to app engine and firebase"
            implementationClass = "org.curioswitch.gradle.plugins.staticsite.StaticSitePlugin"
        }
        register("web") {
            id = "org.curioswitch.gradle-curio-web-plugin"
            displayName = "Gradle Curio Web Plugin"
            description = "Plugin for building web client code for packaging into a server"
            implementationClass = "org.curioswitch.gradle.plugins.curioweb.CurioWebPlugin"
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("pluginMaven") {
            pom {
                name.set("Gradle Curiostack Plugin")
                description.set("A highly opinionated plugin for setting up a gradle codebase " +
                        "using Curiostack conventions. Defines shared configuration and " +
                        "applies other useful plugins in an aim to be the only plugin in a " +
                        "root project.")
                url.set("https://github.com/infostellarinc/curiostack/tree/master/" +
                        "tools/gradle-plugins/gradle-curiostack-plugin")
            }
        }
    }
    repositories {
        maven {
            url = URI(rootProject.findProperty("org.curioswitch.curiostack.repo_uri") as String)
        }
    }
}

tasks.withType(Test::class) {
    enabled = (findProperty("enableCuriostackPluginTests") == "true")

    jvmArgs("-Dorg.curioswitch.curiostack.testing.buildDir=${rootProject.buildDir}")

    // TODO(choko): Have curiostack plugin do this.
    testLogging {
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

configure<SpotlessExtension> {
    java {
        targetExclude("**/test-projects/**")
    }
}
