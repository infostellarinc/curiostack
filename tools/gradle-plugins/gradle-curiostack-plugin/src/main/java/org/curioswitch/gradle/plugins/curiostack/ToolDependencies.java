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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import org.gradle.api.Project;

public class ToolDependencies {

  private static final Map<String, String> DEFAULT_VERSIONS =
      ImmutableMap.<String, String>builder()
          .put("bom", "0.9.1")
          .put("gcloud", "351.0.0")
          .put("golang", "1.16.7")
          .put("google-java-format", "1.15.0")
          .put("gradle", "7.4.1")
          .put("miniconda", "Miniconda3-py39_4.10.3")
          .put("node", "14.17.4")
          .put("openjdk", "17")
          .put("yarn", "1.22.5")
          .build();

  public static String getBomVersion(Project project) {
    return getVersion("bom", project);
  }

  public static String getGcloudVersion(Project project) {
    return getVersion("gcloud", project);
  }

  public static String getGoogleJavaFormatVersion(Project project) {
    return getVersion("google-java-format", project);
  }

  public static String getGradleVersion(Project project) {
    return getVersion("gradle", project);
  }

  public static String getGolangVersion(Project project) {
    return getVersion("golang", project);
  }

  public static String getMinicondaVersion(Project project) {
    return getVersion("miniconda", project);
  }

  public static String getNodeVersion(Project project) {
    return getVersion("node", project);
  }

  public static String getOpenJdkVersion(Project project) {
    return getVersion("openjdk", project);
  }

  public static String getYarnVersion(Project project) {
    return getVersion("yarn", project);
  }

  public static String getDefaultVersion(String tool) {
    return DEFAULT_VERSIONS.getOrDefault(tool, "");
  }

  private static String getVersion(String tool, Project project) {
    return Objects.requireNonNullElse(
        (String) project.getRootProject().findProperty("org.curioswitch.curiostack.tools." + tool),
        DEFAULT_VERSIONS.getOrDefault(tool, ""));
  }

  private ToolDependencies() {}
}
