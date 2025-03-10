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

// Includes work from:
/*
 * Copyright (c) 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.curioswitch.common.testing.assertj.proto;

import javax.annotation.Nullable;

/**
 * A generic entity in the {@link DiffResult} tree with queryable properties.
 *
 * <p>This class is not directly extensible. Only the inner classes, {@link
 * RecursableDiffEntity.WithoutResultCode} and {@link RecursableDiffEntity.WithResultCode}, can be
 * extended.
 *
 * <p>A {@code RecursableDiffEntity}'s base properties (i.e., {@link #isMatched()}, {@link
 * #isIgnored()}) are determined differently depending on the subtype. The {@code WithoutResultCode}
 * subtype derives its base properties entirely from its children, while the {@code WithResultCode}
 * subtype derives its base properties from an enum explicitly set on the entity. The {@link
 * #isAnyChildMatched()} and {@link #isAnyChildIgnored()} properties are determined recursively on
 * both subtypes.
 *
 * <p>A {@code RecursableDiffEntity} may have no children. The nature and count of an entity's
 * children depends on the implementation - see {@link DiffResult} for concrete instances.
 */
abstract class RecursableDiffEntity {

  // Lazily-initialized return values for the resursive properties of the entity.
  // null = not initialized yet
  //
  // This essentially implements what @Memoized does, but @AutoValue doesn't support @Memoized on
  // parent classes.  I think it's better to roll-our-own in the parent class to take advantage of
  // inheritance, than to duplicate the @Memoized methods for every subclass.

  @Nullable private Boolean isAnyChildIgnored = null;
  @Nullable private Boolean isAnyChildMatched = null;

  // Only extended by inner classes.
  private RecursableDiffEntity() {}

  /**
   * The children of this entity. May be empty.
   *
   * <p>Subclasses should {@link com.google.auto.value.extension.memoized.Memoized} this method
   * especially if it's expensive.
   */
  abstract Iterable<? extends RecursableDiffEntity> childEntities();

  /** Returns whether or not the two entities matched according to the diff rules. */
  abstract boolean isMatched();

  /** Returns true if all sub-fields of both entities were ignored for comparison. */
  abstract boolean isIgnored();

  /**
   * Returns true if some child entity matched.
   *
   * <p>Caches the result for future calls.
   */
  final boolean isAnyChildMatched() {
    if (isAnyChildMatched == null) {
      isAnyChildMatched = false;
      for (RecursableDiffEntity entity : childEntities()) {
        if ((entity.isMatched() && !entity.isContentEmpty()) || entity.isAnyChildMatched()) {
          isAnyChildMatched = true;
          break;
        }
      }
    }
    return isAnyChildMatched;
  }

  /**
   * Returns true if some child entity was ignored.
   *
   * <p>Caches the result for future calls.
   */
  final boolean isAnyChildIgnored() {
    if (isAnyChildIgnored == null) {
      isAnyChildIgnored = false;
      for (RecursableDiffEntity entity : childEntities()) {
        if ((entity.isIgnored() && !entity.isContentEmpty()) || entity.isAnyChildIgnored()) {
          isAnyChildIgnored = true;
          break;
        }
      }
    }
    return isAnyChildIgnored;
  }

  /**
   * Prints the contents of this diff entity to {@code sb}.
   *
   * @param includeMatches Whether to include reports for fields which matched.
   * @param fieldPrefix The human-readable field path leading to this entity. Empty if this is the
   *     root entity.
   * @param sb Builder to print the text to.
   */
  abstract void printContents(boolean includeMatches, String fieldPrefix, StringBuilder sb);

  /** Returns true if this entity has no contents to print, with or without includeMatches. */
  abstract boolean isContentEmpty();

  final void printChildContents(boolean includeMatches, String fieldPrefix, StringBuilder sb) {
    for (RecursableDiffEntity entity : childEntities()) {
      entity.printContents(includeMatches, fieldPrefix, sb);
    }
  }

  /**
   * A generic entity in the {@link DiffResult} tree without a result code.
   *
   * <p>This entity derives its {@code isMatched()} and {@code isIgnored()} state purely from its
   * children. If it has no children, it is considered both matched and ignored.
   */
  abstract static class WithoutResultCode extends RecursableDiffEntity {

    @Nullable private Boolean isMatched = null;
    @Nullable private Boolean isIgnored = null;

    @Override
    final boolean isMatched() {
      if (isMatched == null) {
        isMatched = true;
        for (RecursableDiffEntity entity : childEntities()) {
          if (!entity.isMatched()) {
            isMatched = false;
            break;
          }
        }
      }
      return isMatched;
    }

    @Override
    final boolean isIgnored() {
      if (isIgnored == null) {
        isIgnored = true;
        for (RecursableDiffEntity entity : childEntities()) {
          if (!entity.isIgnored()) {
            isIgnored = false;
            break;
          }
        }
      }
      return isIgnored;
    }
  }

  /**
   * A generic entity in the {@link DiffResult} tree with a result code.
   *
   * <p>The result code overrides {@code isMatched()} and {@code isIgnored()} evaluation, using the
   * provided enum instead of any child states.
   */
  abstract static class WithResultCode extends RecursableDiffEntity {
    enum Result {
      /** No differences. The expected case. */
      MATCHED,

      /** expected() didn't have this field, actual() did. */
      ADDED,

      /** actual() didn't have this field, expected() did. */
      REMOVED,

      /** Both messages had the field but the values don't match. */
      MODIFIED,

      /**
       * The message was moved from one index to another, but strict ordering was expected.
       *
       * <p>This is only possible on {@link DiffResult.RepeatedField.PairResult}.
       */
      MOVED_OUT_OF_ORDER,

      /**
       * The messages were ignored for the sake of comparison.
       *
       * <p>IGNORED fields should also be considered MATCHED, for the sake of pass/fail decisions.
       * The IGNORED information is useful for limiting diff output: i.e., if all fields in a deep
       * submessage-to-submessage comparison are ignored, we can print the top-level type as ignored
       * and omit diff lines for the rest of the fields within.
       */
      IGNORED;

      static Builder builder() {
        return new Builder();
      }

      /**
       * A helper class for computing a {@link Result}. It defaults to {@code MATCHED}, but can be
       * changed exactly once if called with a true {@code condition}.
       *
       * <p>All subsequent 'mark' calls after a successful mark are ignored.
       */
      static final class Builder {
        private Result state = Result.MATCHED;

        private Builder() {}

        public void markAddedIf(boolean condition) {
          setIf(condition, Result.ADDED);
        }

        public void markRemovedIf(boolean condition) {
          setIf(condition, Result.REMOVED);
        }

        public void markModifiedIf(boolean condition) {
          setIf(condition, Result.MODIFIED);
        }

        public Result build() {
          return state;
        }

        private void setIf(boolean condition, Result newState) {
          if (condition && state == Result.MATCHED) {
            state = newState;
          }
        }
      }
    }

    abstract Result result();

    @Override
    final boolean isMatched() {
      return result() == Result.MATCHED || result() == Result.IGNORED;
    }

    @Override
    final boolean isIgnored() {
      return result() == Result.IGNORED;
    }
  }
}
