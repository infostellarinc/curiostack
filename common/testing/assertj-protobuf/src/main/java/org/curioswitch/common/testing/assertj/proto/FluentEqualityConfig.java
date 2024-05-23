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
 * Copyright (c) 2016 Google, Inc.
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

import static com.google.common.base.Preconditions.checkState;
import static org.curioswitch.common.testing.assertj.proto.FieldScopeUtil.join;

import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import javax.annotation.Nullable;
import org.assertj.core.data.Offset;

/**
 * A specification for a {@link ProtoTruthMessageDifferencer} for comparing two individual
 * protobufs.}.
 */
@SuppressWarnings("ConstructorLeaksThis") // Not a constructor
@AutoValue
public abstract class FluentEqualityConfig
    implements FieldScopeLogicContainer<FluentEqualityConfig> {

  public static final FluentEqualityConfig DEFAULT_INSTANCE =
      new AutoValue_FluentEqualityConfig.Builder()
          .setIgnoreFieldAbsenceScope(FieldScopeLogic.none())
          .setIgnoreRepeatedFieldOrderScope(FieldScopeLogic.none())
          .setIgnoreExtraRepeatedFieldElementsScope(FieldScopeLogic.none())
          .setDoubleCorrespondenceMap(FieldScopeLogicMap.<Offset<Double>>empty())
          .setFloatCorrespondenceMap(FieldScopeLogicMap.<Offset<Float>>empty())
          .setCompareExpectedFieldsOnly(false)
          .setCompareFieldsScope(FieldScopeLogic.all())
          .setReportMismatchesOnly(false)
          .setUsingCorrespondenceStringFunction(Functions.constant(""))
          .build();

  static FluentEqualityConfig defaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private final LoadingCache<Descriptor, ProtoTruthMessageDifferencer> messageDifferencers =
      CacheBuilder.newBuilder()
          .build(
              new CacheLoader<Descriptor, ProtoTruthMessageDifferencer>() {
                @Override
                public ProtoTruthMessageDifferencer load(Descriptor descriptor) {
                  return ProtoTruthMessageDifferencer.create(FluentEqualityConfig.this, descriptor);
                }
              });

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Storage of AbstractProtoFluentEquals configuration data.
  //////////////////////////////////////////////////////////////////////////////////////////////////

  abstract FieldScopeLogic ignoreFieldAbsenceScope();

  abstract FieldScopeLogic ignoreRepeatedFieldOrderScope();

  abstract FieldScopeLogic ignoreExtraRepeatedFieldElementsScope();

  abstract FieldScopeLogicMap<Offset<Double>> doubleCorrespondenceMap();

  abstract FieldScopeLogicMap<Offset<Float>> floatCorrespondenceMap();

  abstract boolean compareExpectedFieldsOnly();

  // The full list of non-null Messages in the 'expected' part of the assertion.  When set, the
  // FieldScopeLogic should be narrowed appropriately if 'compareExpectedFieldsOnly()' is true.
  //
  // This field will be absent while the assertion is being composed, but *must* be set before
  // passed to a message differencer.  We check this to ensure no assertion path forgets to pass
  // along the expected protos.
  abstract Optional<ImmutableList<Message>> expectedMessages();

  abstract FieldScopeLogic compareFieldsScope();

  abstract boolean reportMismatchesOnly();

  // For pretty-printing, does not affect behavior.
  abstract Function<? super Optional<Descriptor>, String> usingCorrespondenceStringFunction();

  @Nullable
  final String usingCorrespondenceString(Optional<Descriptor> descriptor) {
    return usingCorrespondenceStringFunction().apply(descriptor);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Mutators of FluentEqualityConfig configuration data.
  //////////////////////////////////////////////////////////////////////////////////////////////////

  final FluentEqualityConfig ignoringFieldAbsence() {
    return toBuilder()
        .setIgnoreFieldAbsenceScope(FieldScopeLogic.all())
        .addUsingCorrespondenceString(".ignoringFieldAbsence()")
        .build();
  }

  final FluentEqualityConfig ignoringFieldAbsenceOfFields(Iterable<Integer> fieldNumbers) {
    return toBuilder()
        .setIgnoreFieldAbsenceScope(
            ignoreFieldAbsenceScope().allowingFieldsNonRecursive(fieldNumbers))
        .addUsingCorrespondenceFieldNumbersString(".ignoringFieldAbsenceOf(%s)", fieldNumbers)
        .build();
  }

  final FluentEqualityConfig ignoringFieldAbsenceOfFieldDescriptors(
      Iterable<FieldDescriptor> fieldDescriptors) {
    return toBuilder()
        .setIgnoreFieldAbsenceScope(
            ignoreFieldAbsenceScope().allowingFieldDescriptorsNonRecursive(fieldDescriptors))
        .addUsingCorrespondenceFieldDescriptorsString(
            ".ignoringFieldAbsenceOf(%s)", fieldDescriptors)
        .build();
  }

  final FluentEqualityConfig ignoringRepeatedFieldOrder() {
    return toBuilder()
        .setIgnoreRepeatedFieldOrderScope(FieldScopeLogic.all())
        .addUsingCorrespondenceString(".ignoringRepeatedFieldOrder()")
        .build();
  }

  final FluentEqualityConfig ignoringRepeatedFieldOrderOfFields(Iterable<Integer> fieldNumbers) {
    return toBuilder()
        .setIgnoreRepeatedFieldOrderScope(
            ignoreRepeatedFieldOrderScope().allowingFieldsNonRecursive(fieldNumbers))
        .addUsingCorrespondenceFieldNumbersString(".ignoringRepeatedFieldOrderOf(%s)", fieldNumbers)
        .build();
  }

  final FluentEqualityConfig ignoringRepeatedFieldOrderOfFieldDescriptors(
      Iterable<FieldDescriptor> fieldDescriptors) {
    return toBuilder()
        .setIgnoreRepeatedFieldOrderScope(
            ignoreRepeatedFieldOrderScope().allowingFieldDescriptorsNonRecursive(fieldDescriptors))
        .addUsingCorrespondenceFieldDescriptorsString(
            ".ignoringRepeatedFieldOrderOf(%s)", fieldDescriptors)
        .build();
  }

  final FluentEqualityConfig ignoringExtraRepeatedFieldElements() {
    return toBuilder()
        .setIgnoreExtraRepeatedFieldElementsScope(FieldScopeLogic.all())
        .addUsingCorrespondenceString(".ignoringExtraRepeatedFieldElements()")
        .build();
  }

  final FluentEqualityConfig ignoringExtraRepeatedFieldElementsOfFields(
      Iterable<Integer> fieldNumbers) {
    return toBuilder()
        .setIgnoreExtraRepeatedFieldElementsScope(
            ignoreExtraRepeatedFieldElementsScope().allowingFieldsNonRecursive(fieldNumbers))
        .addUsingCorrespondenceFieldNumbersString(
            ".ignoringExtraRepeatedFieldElements(%s)", fieldNumbers)
        .build();
  }

  final FluentEqualityConfig ignoringExtraRepeatedFieldElementsOfFieldDescriptors(
      Iterable<FieldDescriptor> fieldDescriptors) {
    return toBuilder()
        .setIgnoreExtraRepeatedFieldElementsScope(
            ignoreExtraRepeatedFieldElementsScope()
                .allowingFieldDescriptorsNonRecursive(fieldDescriptors))
        .addUsingCorrespondenceFieldDescriptorsString(
            ".ignoringExtraRepeatedFieldElements(%s)", fieldDescriptors)
        .build();
  }

  final FluentEqualityConfig usingDoubleTolerance(double tolerance) {
    return toBuilder()
        .setDoubleCorrespondenceMap(FieldScopeLogicMap.defaultValue(Offset.strictOffset(tolerance)))
        .addUsingCorrespondenceString(".usingDoubleTolerance(" + tolerance + ")")
        .build();
  }

  final FluentEqualityConfig usingDoubleToleranceForFields(
      double tolerance, Iterable<Integer> fieldNumbers) {
    return toBuilder()
        .setDoubleCorrespondenceMap(
            doubleCorrespondenceMap()
                .with(
                    FieldScopeLogic.none().allowingFieldsNonRecursive(fieldNumbers),
                    Offset.strictOffset(tolerance)))
        .addUsingCorrespondenceFieldNumbersString(
            ".usingDoubleTolerance(" + tolerance + ", %s)", fieldNumbers)
        .build();
  }

  final FluentEqualityConfig usingDoubleToleranceForFieldDescriptors(
      double tolerance, Iterable<FieldDescriptor> fieldDescriptors) {
    return toBuilder()
        .setDoubleCorrespondenceMap(
            doubleCorrespondenceMap()
                .with(
                    FieldScopeLogic.none().allowingFieldDescriptorsNonRecursive(fieldDescriptors),
                    Offset.strictOffset(tolerance)))
        .addUsingCorrespondenceFieldDescriptorsString(
            ".usingDoubleTolerance(" + tolerance + ", %s)", fieldDescriptors)
        .build();
  }

  final FluentEqualityConfig usingFloatTolerance(float tolerance) {
    return toBuilder()
        .setFloatCorrespondenceMap(FieldScopeLogicMap.defaultValue(Offset.strictOffset(tolerance)))
        .addUsingCorrespondenceString(".usingFloatTolerance(" + tolerance + ")")
        .build();
  }

  final FluentEqualityConfig usingFloatToleranceForFields(
      float tolerance, Iterable<Integer> fieldNumbers) {
    return toBuilder()
        .setFloatCorrespondenceMap(
            floatCorrespondenceMap()
                .with(
                    FieldScopeLogic.none().allowingFieldsNonRecursive(fieldNumbers),
                    Offset.strictOffset(tolerance)))
        .addUsingCorrespondenceFieldNumbersString(
            ".usingFloatTolerance(" + tolerance + ", %s)", fieldNumbers)
        .build();
  }

  final FluentEqualityConfig usingFloatToleranceForFieldDescriptors(
      float tolerance, Iterable<FieldDescriptor> fieldDescriptors) {
    return toBuilder()
        .setFloatCorrespondenceMap(
            floatCorrespondenceMap()
                .with(
                    FieldScopeLogic.none().allowingFieldDescriptorsNonRecursive(fieldDescriptors),
                    Offset.strictOffset(tolerance)))
        .addUsingCorrespondenceFieldDescriptorsString(
            ".usingFloatTolerance(" + tolerance + ", %s)", fieldDescriptors)
        .build();
  }

  final FluentEqualityConfig comparingExpectedFieldsOnly() {
    return toBuilder()
        .setCompareExpectedFieldsOnly(true)
        .addUsingCorrespondenceString(".comparingExpectedFieldsOnly()")
        .build();
  }

  final FluentEqualityConfig withExpectedMessages(Iterable<? extends Message> messages) {
    ImmutableList.Builder<Message> listBuilder = ImmutableList.builder();
    for (Message message : messages) {
      if (message != null) {
        listBuilder.add(message);
      }
    }
    Builder builder = toBuilder().setExpectedMessages(listBuilder.build());
    if (compareExpectedFieldsOnly()) {
      builder.setCompareFieldsScope(
          FieldScopeLogic.and(compareFieldsScope(), FieldScopes.fromSetFields(messages).logic()));
    }
    return builder.build();
  }

  final FluentEqualityConfig withPartialScope(FieldScope partialScope) {
    return toBuilder()
        .setCompareFieldsScope(FieldScopeLogic.and(compareFieldsScope(), partialScope.logic()))
        .addUsingCorrespondenceFieldScopeString(".withPartialScope(%s)", partialScope)
        .build();
  }

  final FluentEqualityConfig ignoringFields(Iterable<Integer> fieldNumbers) {
    return toBuilder()
        .setCompareFieldsScope(compareFieldsScope().ignoringFields(fieldNumbers))
        .addUsingCorrespondenceFieldNumbersString(".ignoringFields(%s)", fieldNumbers)
        .build();
  }

  final FluentEqualityConfig ignoringFieldDescriptors(Iterable<FieldDescriptor> fieldDescriptors) {
    return toBuilder()
        .setCompareFieldsScope(compareFieldsScope().ignoringFieldDescriptors(fieldDescriptors))
        .addUsingCorrespondenceFieldDescriptorsString(
            ".ignoringFieldDescriptors(%s)", fieldDescriptors)
        .build();
  }

  final FluentEqualityConfig ignoringFieldScope(FieldScope fieldScope) {
    return toBuilder()
        .setCompareFieldsScope(
            FieldScopeLogic.and(compareFieldsScope(), FieldScopeLogic.not(fieldScope.logic())))
        .addUsingCorrespondenceFieldScopeString(".ignoringFieldScope(%s)", fieldScope)
        .build();
  }

  final FluentEqualityConfig reportingMismatchesOnly() {
    return toBuilder()
        .setReportMismatchesOnly(true)
        .addUsingCorrespondenceString(".reportingMismatchesOnly()")
        .build();
  }

  @Override
  public final FluentEqualityConfig subScope(
      Descriptor rootDescriptor, FieldDescriptorOrUnknown fieldDescriptorOrUnknown) {
    return toBuilder()
        .setIgnoreFieldAbsenceScope(
            ignoreFieldAbsenceScope().subScope(rootDescriptor, fieldDescriptorOrUnknown))
        .setIgnoreRepeatedFieldOrderScope(
            ignoreRepeatedFieldOrderScope().subScope(rootDescriptor, fieldDescriptorOrUnknown))
        .setIgnoreExtraRepeatedFieldElementsScope(
            ignoreExtraRepeatedFieldElementsScope()
                .subScope(rootDescriptor, fieldDescriptorOrUnknown))
        .setDoubleCorrespondenceMap(
            doubleCorrespondenceMap().subScope(rootDescriptor, fieldDescriptorOrUnknown))
        .setFloatCorrespondenceMap(
            floatCorrespondenceMap().subScope(rootDescriptor, fieldDescriptorOrUnknown))
        .setCompareFieldsScope(
            compareFieldsScope().subScope(rootDescriptor, fieldDescriptorOrUnknown))
        .build();
  }

  @Override
  public final void validate(
      Descriptor rootDescriptor, FieldDescriptorValidator fieldDescriptorValidator) {
    // FluentEqualityConfig should never be validated other than as a root entity.
    Verify.verify(fieldDescriptorValidator == FieldDescriptorValidator.ALLOW_ALL);

    ignoreFieldAbsenceScope()
        .validate(rootDescriptor, FieldDescriptorValidator.IS_FIELD_WITH_ABSENCE);
    ignoreRepeatedFieldOrderScope()
        .validate(rootDescriptor, FieldDescriptorValidator.IS_FIELD_WITH_ORDER);
    ignoreExtraRepeatedFieldElementsScope()
        .validate(rootDescriptor, FieldDescriptorValidator.IS_FIELD_WITH_EXTRA_ELEMENTS);
    doubleCorrespondenceMap().validate(rootDescriptor, FieldDescriptorValidator.IS_DOUBLE_FIELD);
    floatCorrespondenceMap().validate(rootDescriptor, FieldDescriptorValidator.IS_FLOAT_FIELD);
    compareFieldsScope().validate(rootDescriptor, FieldDescriptorValidator.ALLOW_ALL);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Converters into comparison utilities.
  //////////////////////////////////////////////////////////////////////////////////////////////////

  final ProtoTruthMessageDifferencer toMessageDifferencer(Descriptor descriptor) {
    checkState(expectedMessages().isPresent(), "expectedMessages() not set");
    return messageDifferencers.getUnchecked(descriptor);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Builder methods.
  //////////////////////////////////////////////////////////////////////////////////////////////////

  abstract Builder toBuilder();

  @AutoValue.Builder
  abstract static class Builder {
    @CanIgnoreReturnValue
    abstract Builder setIgnoreFieldAbsenceScope(FieldScopeLogic fieldScopeLogic);

    @CanIgnoreReturnValue
    abstract Builder setIgnoreRepeatedFieldOrderScope(FieldScopeLogic fieldScopeLogic);

    @CanIgnoreReturnValue
    abstract Builder setIgnoreExtraRepeatedFieldElementsScope(FieldScopeLogic fieldScopeLogic);

    @CanIgnoreReturnValue
    abstract Builder setDoubleCorrespondenceMap(
        FieldScopeLogicMap<Offset<Double>> doubleCorrespondenceMap);

    @CanIgnoreReturnValue
    abstract Builder setFloatCorrespondenceMap(
        FieldScopeLogicMap<Offset<Float>> floatCorrespondenceMap);

    @CanIgnoreReturnValue
    abstract Builder setCompareExpectedFieldsOnly(boolean compare);

    @CanIgnoreReturnValue
    abstract Builder setExpectedMessages(ImmutableList<Message> messages);

    @CanIgnoreReturnValue
    abstract Builder setCompareFieldsScope(FieldScopeLogic fieldScopeLogic);

    @CanIgnoreReturnValue
    abstract Builder setReportMismatchesOnly(boolean reportMismatchesOnly);

    @CheckReturnValue
    abstract Function<? super Optional<Descriptor>, String> usingCorrespondenceStringFunction();

    @CanIgnoreReturnValue
    abstract Builder setUsingCorrespondenceStringFunction(
        Function<? super Optional<Descriptor>, String> usingCorrespondenceStringFunction);

    @CanIgnoreReturnValue
    abstract FluentEqualityConfig build();

    // Lazy formatting methods.
    // These allow us to print raw integer field numbers with meaningful names.

    @CanIgnoreReturnValue
    final Builder addUsingCorrespondenceString(String string) {
      return setUsingCorrespondenceStringFunction(
          FieldScopeUtil.concat(usingCorrespondenceStringFunction(), Functions.constant(string)));
    }

    @CanIgnoreReturnValue
    final Builder addUsingCorrespondenceFieldNumbersString(
        String fmt, Iterable<Integer> fieldNumbers) {
      return setUsingCorrespondenceStringFunction(
          FieldScopeUtil.concat(
              usingCorrespondenceStringFunction(),
              FieldScopeUtil.fieldNumbersFunction(fmt, fieldNumbers)));
    }

    @CanIgnoreReturnValue
    final Builder addUsingCorrespondenceFieldDescriptorsString(
        String fmt, Iterable<FieldDescriptor> fieldDescriptors) {
      return setUsingCorrespondenceStringFunction(
          FieldScopeUtil.concat(
              usingCorrespondenceStringFunction(),
              Functions.constant(String.format(fmt, join(fieldDescriptors)))));
    }

    @CanIgnoreReturnValue
    final Builder addUsingCorrespondenceFieldScopeString(String fmt, FieldScope fieldScope) {
      return setUsingCorrespondenceStringFunction(
          FieldScopeUtil.concat(
              usingCorrespondenceStringFunction(),
              FieldScopeUtil.fieldScopeFunction(fmt, fieldScope)));
    }
  }
}
