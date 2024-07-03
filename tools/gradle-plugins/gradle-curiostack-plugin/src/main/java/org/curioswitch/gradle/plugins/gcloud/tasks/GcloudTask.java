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

package org.curioswitch.gradle.plugins.gcloud.tasks;

import com.google.common.collect.ImmutableList;
import org.curioswitch.gradle.plugins.gcloud.GcloudExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;

/** A {@link org.gradle.api.Task} that executes a gcloud sdk command. */
public class GcloudTask extends DefaultTask {

  private final ListProperty<Object> args;

  @SuppressWarnings("ConstructorLeaksThis")
  public GcloudTask() {
    args = getProject().getObjects().listProperty(Object.class).empty();
  }

  public void setArgs(Iterable<?> args) {
    this.args.set(args);
  }

  public void args(Iterable<?> args) {
    this.args.addAll(args);
  }

  public void args(Provider<Iterable<?>> args) {
    this.args.addAll(args);
  }

  @TaskAction
  public void exec() {
    GcloudExtension config =
        getProject().getRootProject().getExtensions().getByType(GcloudExtension.class);

    ImmutableList.Builder<Object> fullArgs =
        ImmutableList.builder().add("--project=" + config.getClusterProject().get()).add("--quiet");

    args.get().stream()
        .map(o -> o instanceof Provider ? ((Provider<?>) o).get() : o)
        .forEachOrdered(fullArgs::add);

    getProject()
        .exec(
            exec -> {
              exec.executable("gcloud");
              exec.args(fullArgs.build());
              exec.setStandardInput(System.in);
            });
  }
}
