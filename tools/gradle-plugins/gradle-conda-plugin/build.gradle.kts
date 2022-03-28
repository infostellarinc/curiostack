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

plugins {
    `java-gradle-plugin`
    `maven-publish`

    id("com.gradle.plugin-publish")
}

dependencies {
    implementation(project(":tools:gradle-plugins:gradle-tool-downloader-plugin"))
    implementation(project(":tools:gradle-plugins:gradle-helpers"))

    implementation("com.google.guava:guava")

    compileOnly(project(":common:curio-helpers"))

    annotationProcessor("org.immutables:value")
    compileOnly("org.immutables:value-annotations")
}

gradlePlugin {
    plugins {
        register("conda") {
            id = "com.stellarstation.curiostack.gradle-conda-plugin"
            displayName = "Gradle Miniconda Plugin"
            description = "Plugin for setting up a Miniconda environment"
            implementationClass = "org.curioswitch.gradle.conda.CondaPlugin"
        }
        register("buildenv") {
            id = "com.stellarstation.curiostack.gradle-conda-buildenv-plugin"
            displayName = "Gradle Conda Build Environment"
            description = "Plugin for configuring the Miniconda environment for builds, downloading toolchains such as C++"
            implementationClass = "org.curioswitch.gradle.conda.CondaBuildEnvPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/curioswitch/curiostack/tree/master/tools/gradle-plugins/gradle-conda-plugin"
    vcsUrl = "https://github.com/curioswitch/curiostack.git"
    tags = listOf("curiostack", "miniconda")
}

publishing {
    publications {
        register<MavenPublication>("pluginMaven") {
            pom {
                name.set("Gradle Conda Plugin")
                description.set("Gradle plugin to use miniconda to create a build environment.")
                url.set("https://github.com/curioswitch/curiostack/tree/master/tools/" +
                        "gradle-plugins/gradle-conda-plugin")
            }
        }
    }
}
