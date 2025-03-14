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

import org.curioswitch.gradle.plugins.curiostack.CuriostackExtension

pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.curioswitch.gradle-curiostack-plugin") {
                useModule("org.curioswitch.curiostack:gradle-curiostack-plugin:${requested.version}")
            }
        }
    }

    plugins {
        id("com.google.cloud.artifactregistry.gradle-plugin") version "2.2.1"
    }

    repositories {
        mavenCentral {
            content {
                excludeModuleByRegex("org\\.curioswitch\\..*", "(?!protobuf-jackson)")
            }
        }
        gradlePluginPortal {
            content {
                excludeModuleByRegex("org\\.curioswitch\\..*", "(?!protobuf-jackson)")
            }
        }
        maven {
            url = uri((extra.properties["org.curioswitch.curiostack.repo_uri"] as String).replace("artifactregistry://", "https://"))
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                username = "oauth2accesstoken"
                password = java.io.ByteArrayOutputStream().use { output ->
                    try {
                        exec {
                            commandLine("gcloud", "auth", "print-access-token")
                            standardOutput = output
                        }
                        output.toString()
                    } catch (e: Throwable) {
                        error("gcloud cli authentication failed: ${e.message}")
                    }
                }
            }
        }
        mavenLocal()
    }
}

plugins {
    id("com.gradle.enterprise").version("3.6.3")
    id("org.curioswitch.gradle-curiostack-plugin").version("0.11.11")
    id("com.google.cloud.artifactregistry.gradle-plugin").version("2.2.1")
}

configure<CuriostackExtension> {
    buildCacheBucket.set("curioswitch-gradle-build-cache")
}
