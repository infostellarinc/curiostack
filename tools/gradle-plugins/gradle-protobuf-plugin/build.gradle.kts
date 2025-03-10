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

import java.net.URI

plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.google.cloud.artifactregistry.gradle-plugin")
}

dependencies {
    implementation(project(":tools:gradle-plugins:gradle-helpers"))

    implementation("com.google.gradle:osdetector-gradle-plugin")
    implementation("com.google.guava:guava")

    compileOnly(project(":common:curio-helpers"))

    annotationProcessor("org.immutables:value")
    compileOnly("org.immutables:value-annotations")
}

gradlePlugin {
    plugins {
        register("protobuf") {
            id = "org.curioswitch.gradle-protobuf-plugin"
            displayName = "Gradle Protobuf Plugin"
            description = "A modern protobuf plugin using Gradle's latest features."
            implementationClass = "org.curioswitch.gradle.protobuf.ProtobufPlugin"
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("pluginMaven") {
            pom {
                name.set("Gradle Protobuf Plugin")
                description.set("Gradle plugin to compile proto files into generated code.")
                url.set("https://github.com/infostellarinc/curiostack/tree/master/tools/" +
                        "gradle-plugins/gradle-protobuf-plugin")
            }
        }
    }
    repositories {
        maven {
            url = URI(rootProject.findProperty("org.curioswitch.curiostack.repo_uri") as String)
        }
    }
}
