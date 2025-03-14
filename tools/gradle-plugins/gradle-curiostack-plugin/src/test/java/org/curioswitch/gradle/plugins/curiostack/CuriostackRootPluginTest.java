/*
 * MIT License
 *
 * Copyright (c) 2019 Choko (choko@curioswitch.org)
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

package org.curioswitch.gradle.plugins.curiostack;

import static org.curioswitch.gradle.testing.assertj.CurioGradleAssertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.curioswitch.gradle.testing.ResourceProjects;
import org.gradle.testkit.runner.GradleRunner;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

class CuriostackRootPluginTest {

  @SuppressWarnings("ClassCanBeStatic")
  @Nested
  class KitchenSink {

    private Path projectDir;

    @BeforeAll
    void copyProject() {
      projectDir =
          ResourceProjects.fromResources("test-projects/gradle-curiostack-plugin/kitchen-sink");
    }

    @Test
    void pomNotManaged() throws Exception {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir.toFile())
                  .withArguments(":java-library1:generatePomFileForMavenPublication")
                  .withPluginClasspath())
          .builds()
          .tasksDidSucceed(":java-library1:generatePomFileForMavenPublication");

      var soup =
          Jsoup.parse(
              Files.readString(
                  projectDir.resolve("java-library1/build/publications/maven/pom-default.xml")));
      assertThat(soup.select("dependencyManagement")).isEmpty();
      assertThat(soup.select("dependency"))
          .hasSize(3)
          .allSatisfy(
              dependency -> {
                assertThat(dependency.selectFirst("groupId").text()).isNotEmpty();
                assertThat(dependency.selectFirst("artifactId").text()).isNotEmpty();
                assertThat(dependency.selectFirst("version").text()).isNotEmpty();
              });
    }

    @Test
    // This test is slow since it runs yarn, just run locally for now.
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    void checksResolutions() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir.toFile())
                  .withArguments(":checkNodeResolutions")
                  .withPluginClasspath())
          .builds()
          .tasksDidSucceed(":checkNodeResolutions");
    }

    @Test
    // This test is slow since it runs yarn, just run locally for now.
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    void updatesResolutions() {
      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir.toFile())
                  .withArguments(":checkNodeResolutions")
                  .withPluginClasspath())
          .fails()
          .tasksDidRun(":checkNodeResolutions");

      assertThat(
              GradleRunner.create()
                  .withProjectDir(projectDir.toFile())
                  .withArguments(":updateNodeResolutions")
                  .withPluginClasspath())
          .builds()
          .tasksDidSucceed(":updateNodeResolutions");
    }
  }
}
