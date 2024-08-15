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
          .put("bom", "0.11.12")
          .put("google-java-format", "1.15.0")
          .build();

  public static String getBomVersion(Project project) {
    return getVersion("bom", project);
  }

  public static String getGoogleJavaFormatVersion(Project project) {
    return getVersion("google-java-format", project);
  }

  private static String getVersion(String tool, Project project) {
    return Objects.requireNonNullElse(
        (String) project.getRootProject().findProperty("org.curioswitch.curiostack.tools." + tool),
        DEFAULT_VERSIONS.getOrDefault(tool, ""));
  }

  private ToolDependencies() {}
}
