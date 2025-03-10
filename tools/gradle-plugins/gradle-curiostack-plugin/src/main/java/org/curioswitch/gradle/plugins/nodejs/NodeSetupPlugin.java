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

package org.curioswitch.gradle.plugins.nodejs;

import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.curioswitch.gradle.plugins.nodejs.tasks.NodeTask;
import org.curioswitch.gradle.plugins.nodejs.tasks.UpdateNodeResolutions;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.Delete;

public class NodeSetupPlugin implements Plugin<Project> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public void apply(Project project) {
    checkState(
        project.getParent() == null, "node-setup-plugin can only be applied to the root project.");

    NodeSetupExtension.create(project);

    project.getPlugins().apply(BasePlugin.class);

    project
        .getTasks()
        .withType(Delete.class)
        .named("clean")
        .configure(t -> t.delete("node_modules"));

    project.getTasks().register(UpdateNodeResolutions.NAME, UpdateNodeResolutions.class, false);
    var checkNodeResolutions =
        project
            .getTasks()
            .register(UpdateNodeResolutions.CHECK_NAME, UpdateNodeResolutions.class, true);

    var yarnWarning =
        project
            .getTasks()
            .register(
                "yarnWarning",
                task ->
                    task.doFirst(
                        unused ->
                            project
                                .getLogger()
                                .warn(
                                    "yarn task failed. If you have updated a dependency and the "
                                        + "error says 'Your lockfile needs to be updated.', run \n\n"
                                        + "./gradlew yarnUpdate")));

    var yarn =
        project
            .getTasks()
            .register(
                "yarn",
                NodeTask.class,
                t -> {
                  t.args("--frozen-lockfile");

                  var packageJsonFile = project.file("package.json");
                  if (packageJsonFile.exists()) {
                    final JsonNode packageJson;
                    try {
                      packageJson = OBJECT_MAPPER.readTree(packageJsonFile);
                    } catch (IOException e) {
                      throw new UncheckedIOException("Could not read package.json", e);
                    }
                    if (packageJson.has("workspaces")) {
                      for (var workspaceNode : packageJson.get("workspaces")) {
                        String workspacePath = workspaceNode.asText();

                        // Assume any workspace in build/web requires a build before running yarn.
                        // This is usually used for generating protos.
                        if (!workspacePath.endsWith("/build/web")) {
                          continue;
                        }

                        String projectPath =
                            workspacePath.substring(
                                0, workspacePath.length() - "/build/web".length());

                        Project workspace =
                            project.findProject(':' + projectPath.replace('/', ':'));
                        if (workspace != null) {
                          t.dependsOn(workspace.getPath() + ":build");
                        }
                      }
                    }
                  }

                  yarnWarning.get().onlyIf(unused -> t.getState().getFailure() != null);
                  t.finalizedBy(yarnWarning, checkNodeResolutions);
                });
    checkNodeResolutions.configure(t -> t.dependsOn(yarn));

    project.getTasks().register("yarnUpdate", NodeTask.class);
  }
}
