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

package org.curioswitch.gradle.plugins.grpcapi;

import com.google.common.io.Resources;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.curioswitch.gradle.plugins.grpcapi.tasks.PackageWebTask;
import org.curioswitch.gradle.plugins.nodejs.NodePlugin;
import org.curioswitch.gradle.protobuf.ProtobufExtension;
import org.curioswitch.gradle.protobuf.ProtobufPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePluginConvention;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

/**
 * A simple gradle plugin that configures the protobuf-gradle-plugin with appropriate defaults for a
 * GRPC API definition.
 *
 * <p>The project will be configured as a Java library with the GRPC dependencies, and the protobuf
 * compiler will generate both Java code and a descriptor set with source code\ info for using in
 * documentation services.
 */
public class GrpcApiPlugin implements Plugin<Project> {

  private static final String PACKAGE_JSON_TEMPLATE;

  static {
    try {
      // Gradle often initializes tasks in a separate classloader that can't read this resource
      // easily so we make sure to load it here instead of in the task which is its only usage.
      PACKAGE_JSON_TEMPLATE =
          Resources.toString(
              Resources.getResource("grpcapi/package-template.json"), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read package-template.json", e);
    }
  }

  private static final List<String> GRPC_DEPENDENCIES =
      Collections.unmodifiableList(Arrays.asList("grpc-core", "grpc-protobuf", "grpc-stub"));

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(JavaLibraryPlugin.class);
    project.getPlugins().apply(ProtobufPlugin.class);
    project.getPlugins().apply(NodePlugin.class);

    var config = GrpcExtension.createAndAdd(project);

    GRPC_DEPENDENCIES.forEach(dep -> project.getDependencies().add("api", "io.grpc:" + dep));
    project.getDependencies().add("compileOnly", "javax.annotation:javax.annotation-api");

    ProtobufExtension protobuf = project.getExtensions().getByType(ProtobufExtension.class);

    protobuf
        .getLanguages()
        .register(
            "grpc",
            language -> language.getPlugin().getArtifact().set("io.grpc:protoc-gen-grpc-java"));

    project.afterEvaluate(
        p -> {
          String archivesBaseName =
              project.getConvention().getPlugin(BasePluginConvention.class).getArchivesBaseName();
          var descriptorOptions = protobuf.getDescriptorSetOptions();
          descriptorOptions
              .getPath()
              .set(
                  project.file(
                      "build/resources/main/META-INF/armeria/grpc/"
                          + project.getGroup()
                          + "."
                          + archivesBaseName
                          + ".dsc"));
          descriptorOptions.getEnabled().set(true);
          descriptorOptions.getIncludeSourceInfo().set(true);
          descriptorOptions.getIncludeImports().set(true);

          if (config.getWeb().get()) {
            protobuf
                .getLanguages()
                .register(
                    "js",
                    language -> {
                      language.option("import_style=commonjs,binary");
                      language.getOutputDir().set(project.file("build/webprotos"));
                    });
            protobuf
                .getLanguages()
                .register(
                    "grpc-web",
                    language -> {
                      language.option("import_style=commonjs+dts");
                      language.option("mode=grpcweb");
                      language.getOutputDir().set(project.file("build/webprotos"));
                    });
          }
        });

    // Additional configuration of tasks created by protobuf plugin.
    project.afterEvaluate(
        p -> {
          if (config.getWeb().get()) {
            var generateProto = project.getTasks().named("generateProto");

            var packageWeb =
                project
                    .getTasks()
                    .register(
                        "packageWeb",
                        PackageWebTask.class,
                        t -> {
                          t.dependsOn(generateProto);
                          t.getWebPackageName().set(config.getWebPackageName());
                          t.getPackageJsonTemplate().set(PACKAGE_JSON_TEMPLATE);
                        });

            generateProto.configure(t -> t.finalizedBy(packageWeb));

            project
                .getRootProject()
                .getTasks()
                .named("yarn")
                .configure(t -> t.dependsOn(generateProto));

            // Unclear why sometimes compileTestJava fails with "no source files" instead of being
            // skipped (usually when activating web), but it's not that hard to at least check the
            // source set directory.
            SourceSetContainer sourceSets =
                project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
            if (sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME).getAllJava().isEmpty()) {
              project.getTasks().named("compileTestJava").configure(t -> t.setEnabled(false));
              project.getTasks().named("test").configure(t -> t.setEnabled(false));
            }
          }
        });
  }
}
