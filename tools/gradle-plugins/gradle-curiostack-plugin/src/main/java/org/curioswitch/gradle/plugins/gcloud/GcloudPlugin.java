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

package org.curioswitch.gradle.plugins.gcloud;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.curioswitch.gradle.plugins.curioserver.CurioServerPlugin;
import org.curioswitch.gradle.plugins.curioserver.ServerExtension;
import org.curioswitch.gradle.plugins.gcloud.keys.KmsKeyDecrypter;
import org.curioswitch.gradle.plugins.gcloud.tasks.GcloudTask;
import org.curioswitch.gradle.plugins.gcloud.tasks.KubectlTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Rule;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import org.immutables.value.Value.Style.BuilderVisibility;
import org.immutables.value.Value.Style.ImplementationVisibility;

/**
 * A plugin that adds tasks for automatically downloading the gcloud sdk and running commands using
 * it from gradle.
 */
public class GcloudPlugin implements Plugin<Project> {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper(
              new YAMLFactory().enable(Feature.MINIMIZE_QUOTES).disable(Feature.SPLIT_LINES))
          .registerModule(new GuavaModule())
          .setSerializationInclusion(Include.NON_EMPTY);

  @Override
  public void apply(Project project) {
    var config = GcloudExtension.create(project);

    project.getExtensions().getExtraProperties().set("keys", new KmsKeyDecrypter(project));

    ExtraPropertiesExtension ext = project.getExtensions().getExtraProperties();
    ext.set(GcloudTask.class.getSimpleName(), GcloudTask.class);

    project
        .getTasks()
        .addRule(
            new Rule() {
              @Override
              public String getDescription() {
                return "Pattern: \"gcloud_<command>\": Executes a Gcloud command.";
              }

              @Override
              public void apply(String taskName) {
                if (taskName.startsWith("gcloud_")) {
                  GcloudTask task = project.getTasks().create(taskName, GcloudTask.class);
                  List<String> tokens = ImmutableList.copyOf(taskName.split("_"));
                  task.setArgs(tokens.subList(1, tokens.size()));
                }
              }
            });

    var gcloudLoginToCluster =
        project
            .getTasks()
            .register(
                "gcloudLoginToCluster",
                GcloudTask.class,
                t ->
                    t.args(
                        ImmutableList.of(
                            "container",
                            "clusters",
                            "get-credentials",
                            config.getClusterName(),
                            config
                                .getClusterRegion()
                                .map(
                                    value ->
                                        System.getenv("CLOUDSDK_COMPUTE_ZONE") != null
                                            ? "--zone=" + System.getenv("CLOUDSDK_COMPUTE_ZONE")
                                            : "--region=" + value))));

    project.allprojects(
        subproj ->
            subproj
                .getTasks()
                .withType(KubectlTask.class)
                .configureEach(
                    t -> {
                      t.dependsOn(gcloudLoginToCluster);
                    }));

    project.allprojects(
        proj ->
            proj.getPlugins()
                .withType(
                    CurioServerPlugin.class,
                    unused -> {
                      ServerExtension server =
                          proj.getExtensions().getByType(ServerExtension.class);
                      server
                          .getImagePrefix()
                          .set(
                              config
                                  .getContainerRegistry()
                                  .map(
                                      value ->
                                          value + "/" + config.getClusterProject().get() + "/"));
                    }));

    addGenerateCloudBuildTask(project, config);
  }

  private static void addGenerateCloudBuildTask(Project rootProject, GcloudExtension config) {
    rootProject
        .getTasks()
        .register(
            "gcloudGenerateCloudBuild",
            task ->
                task.doLast(
                    t -> {
                      File existingCloudbuildFile = rootProject.file("cloudbuild.yaml");
                      final CloudBuild existingCloudBuild;
                      try {
                        existingCloudBuild =
                            !existingCloudbuildFile.exists()
                                ? null
                                : OBJECT_MAPPER.readValue(existingCloudbuildFile, CloudBuild.class);
                      } catch (IOException e) {
                        throw new UncheckedIOException(
                            "Could not parse existing cloudbuild file.", e);
                      }

                      String deepenGitRepoId = "curio-generated-deepen-git-repo";
                      String fetchUncompressedCacheId =
                          "curio-generated-fetch-uncompressed-build-cache";
                      String fetchCompressedCacheId =
                          "curio-generated-fetch-compressed-build-cache";

                      String buildAllImageId = "curio-generated-build-all";

                      var fetchUncompressedCacheStep =
                          ImmutableCloudBuildStep.builder()
                              .id(fetchUncompressedCacheId)
                              .addWaitFor("-")
                              .name("gcr.io/cloud-builders/gsutil")
                              .entrypoint("bash")
                              .addArgs(
                                  "-c",
                                  "gsutil cp gs://"
                                      + config.getBuildCacheStorageBucket().get()
                                      + "/cloudbuild-cache-uncompressed.tar .gradle/cloudbuild-cache-uncompressed.tar || echo Could not fetch uncompressed build cache...")
                              .build();
                      var fetchCompressedCacheStep =
                          ImmutableCloudBuildStep.builder()
                              .id(fetchCompressedCacheId)
                              .addWaitFor("-")
                              .name("gcr.io/cloud-builders/gsutil")
                              .entrypoint("bash")
                              .addArgs(
                                  "-c",
                                  "gsutil cp gs://"
                                      + config.getBuildCacheStorageBucket().get()
                                      + "/cloudbuild-cache-compressed.tar.gz .gradle/cloudbuild-cache-compressed.tar.gz || echo Could not fetch compressed build cache...")
                              .build();

                      List<CloudBuildStep> steps = new ArrayList<>();
                      steps.add(
                          ImmutableCloudBuildStep.builder()
                              .id(deepenGitRepoId)
                              .addWaitFor("-")
                              .name("gcr.io/cloud-builders/git")
                              .args(ImmutableList.of("fetch", "origin", "master", "--depth=10"))
                              .build());
                      steps.add(fetchUncompressedCacheStep);
                      steps.add(fetchCompressedCacheStep);
                      steps.add(
                          ImmutableCloudBuildStep.builder()
                              .id(buildAllImageId)
                              .addWaitFor(
                                  deepenGitRepoId, fetchUncompressedCacheId, fetchCompressedCacheId)
                              .name("openjdk:10-jdk-slim")
                              .entrypoint("bash")
                              .addArgs(
                                  "-c",
                                  "(test -e .gradle/cloudbuild-cache-uncompressed.tar && tar -xpPf .gradle/cloudbuild-cache-uncompressed.tar && tar -xpPf .gradle/cloudbuild-cache-compressed.tar.gz || echo No build cache yet.) && ./gradlew continuousBuild --stacktrace --no-daemon -Pcuriostack.revisionId=$REVISION_ID && tar -cpPf .gradle/cloudbuild-cache-uncompressed.tar /root/.gradle/wrapper /root/.gradle/caches /root/.gradle/curiostack && tar -cpPzf .gradle/cloudbuild-cache-compressed.tar.gz /usr/local/share/.cache")
                              .env(
                                  ImmutableList.of(
                                      "CI=true",
                                      "CI_MASTER=true",
                                      "CLOUDSDK_COMPUTE_ZONE=" + config.getClusterRegion().get()))
                              .build());
                      steps.add(
                          ImmutableCloudBuildStep.builder()
                              .id("curio-generated-push-uncompressed-build-cache")
                              .addWaitFor(buildAllImageId)
                              .name("gcr.io/cloud-builders/gsutil")
                              .addArgs(
                                  "-o",
                                  "GSUtil:parallel_composite_upload_threshold=150M",
                                  "cp",
                                  ".gradle/cloudbuild-cache-uncompressed.tar",
                                  "gs://"
                                      + config.getBuildCacheStorageBucket().get()
                                      + "/cloudbuild-cache-uncompressed.tar")
                              .build());
                      steps.add(
                          ImmutableCloudBuildStep.builder()
                              .id("curio-generated-push-compressed-build-cache")
                              .addWaitFor(buildAllImageId)
                              .name("gcr.io/cloud-builders/gsutil")
                              .addArgs(
                                  "-o",
                                  "GSUtil:parallel_composite_upload_threshold=150M",
                                  "cp",
                                  ".gradle/cloudbuild-cache-compressed.tar.gz",
                                  "gs://"
                                      + config.getBuildCacheStorageBucket().get()
                                      + "/cloudbuild-cache-compressed.tar.gz")
                              .build());

                      ImmutableCloudBuild.Builder cloudBuildConfig =
                          ImmutableCloudBuild.builder().addAllSteps(steps);

                      if (existingCloudBuild != null) {
                        CloudBuild existingWithoutGenerated =
                            ImmutableCloudBuild.builder()
                                .from(existingCloudBuild)
                                .steps(
                                    existingCloudBuild.steps().stream()
                                            .filter(
                                                step -> !step.id().startsWith("curio-generated-"))
                                        ::iterator)
                                .images(existingCloudBuild.images())
                                .build();
                        cloudBuildConfig.from(existingWithoutGenerated);
                      }

                      try {
                        OBJECT_MAPPER.writeValue(
                            rootProject.file("cloudbuild.yaml"), cloudBuildConfig.build());
                      } catch (IOException e) {
                        throw new UncheckedIOException(e);
                      }

                      CloudBuild releaseCloudBuild =
                          ImmutableCloudBuild.builder()
                              .addSteps(
                                  fetchUncompressedCacheStep,
                                  fetchCompressedCacheStep,
                                  ImmutableCloudBuildStep.builder()
                                      .id("curio-generated-build-releases")
                                      .addWaitFor(fetchUncompressedCacheId, fetchCompressedCacheId)
                                      .name("openjdk:10-jdk-slim")
                                      .entrypoint("bash")
                                      .addArgs(
                                          "-c",
                                          "(test -e .gradle/cloudbuild-cache-uncompressed.tar && tar -xpPf .gradle/cloudbuild-cache-uncompressed.tar && tar -xpPf .gradle/cloudbuild-cache-compressed.tar.gz || echo No build cache yet.) && ./gradlew releaseBuild --stacktrace --no-daemon")
                                      .env(
                                          ImmutableList.of(
                                              "CI=true",
                                              "TAG_NAME=$TAG_NAME",
                                              "BRANCH_NAME=$BRANCH_NAME"))
                                      .build())
                              .build();
                      try {
                        OBJECT_MAPPER.writeValue(
                            rootProject.file("cloudbuild-release.yaml"), releaseCloudBuild);
                      } catch (IOException e) {
                        throw new UncheckedIOException(e);
                      }
                    }));
  }

  @Immutable
  @Style(
      visibility = ImplementationVisibility.PACKAGE,
      builderVisibility = BuilderVisibility.PACKAGE,
      defaultAsDefault = true)
  @JsonDeserialize(as = ImmutableCloudBuildStep.class)
  @JsonSerialize(as = ImmutableCloudBuildStep.class)
  interface CloudBuildStep {

    String id();

    default List<String> waitFor() {
      return ImmutableList.of();
    }

    String name();

    @Nullable
    default String entrypoint() {
      return null;
    }

    List<String> args();

    default List<String> env() {
      return ImmutableList.of("CI=true");
    }
    ;
  }

  @Immutable
  @Style(
      visibility = ImplementationVisibility.PACKAGE,
      builderVisibility = BuilderVisibility.PACKAGE,
      defaultAsDefault = true)
  @JsonDeserialize(as = ImmutableCloudBuild.class)
  @JsonSerialize(as = ImmutableCloudBuild.class)
  interface CloudBuild {
    List<CloudBuildStep> steps();

    List<String> images();

    @Nullable
    default String timeout() {
      return null;
    }

    Map<String, String> options();
  }
}
