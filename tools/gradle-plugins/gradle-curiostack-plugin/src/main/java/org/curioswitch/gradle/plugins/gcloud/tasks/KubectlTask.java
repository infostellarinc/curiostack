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

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.TaskAction;

/** A {@link org.gradle.api.Task} that executes a kubectl command. */
public class KubectlTask extends DefaultTask {

  private final ListProperty<String> args;

  private boolean ignoreExitValue;

  public KubectlTask() {
    setGroup("gcloud");

    args = getProject().getObjects().listProperty(String.class).empty();
  }

  public KubectlTask setArgs(Iterable<String> args) {
    this.args.set(args);
    return this;
  }

  public KubectlTask setArgs(ListProperty<String> args) {
    this.args.set(args);
    return this;
  }

  public KubectlTask setIgnoreExitValue(boolean ignoreExitValue) {
    this.ignoreExitValue = ignoreExitValue;
    return this;
  }

  @TaskAction
  public void exec() {
    getProject()
        .exec(
            exec -> {
              exec.executable("kubectl");
              exec.args(args.get());
              exec.setStandardInput(System.in);
              exec.setIgnoreExitValue(ignoreExitValue);
            });
  }
}
