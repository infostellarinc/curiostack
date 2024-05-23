/*
 * MIT License
 *
 * Copyright (c) 2022 Choko (choko@curioswitch.org)
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

package org.curioswitch.common.server.framework.database.smf;

import static org.simpleflatmapper.util.Asserts.requireNonNull;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.simpleflatmapper.reflect.BuilderInstantiatorDefinition;
import org.simpleflatmapper.reflect.InstantiatorDefinition;
import org.simpleflatmapper.reflect.InstantiatorFactory;
import org.simpleflatmapper.reflect.ObjectGetterFactory;
import org.simpleflatmapper.reflect.ObjectSetterFactory;
import org.simpleflatmapper.reflect.ReflectionInstantiatorDefinitionFactory;
import org.simpleflatmapper.reflect.ReflectionService;
import org.simpleflatmapper.reflect.Setter;
import org.simpleflatmapper.reflect.asm.AsmFactory;
import org.simpleflatmapper.reflect.impl.BuilderInstantiatorDefinitionFactory;
import org.simpleflatmapper.reflect.impl.ImmutableOrgHelper;
import org.simpleflatmapper.reflect.impl.JavaLangClassMetaFactoryProducer;
import org.simpleflatmapper.reflect.instantiator.ExecutableInstantiatorDefinition;
import org.simpleflatmapper.reflect.instantiator.InstantiatorDefinitions;
import org.simpleflatmapper.reflect.instantiator.KotlinDefaultConstructorInstantiatorDefinition;
import org.simpleflatmapper.reflect.meta.AliasProvider;
import org.simpleflatmapper.reflect.meta.AliasProviderService;
import org.simpleflatmapper.reflect.meta.ArrayClassMeta;
import org.simpleflatmapper.reflect.meta.ClassMeta;
import org.simpleflatmapper.reflect.meta.FastTupleClassMeta;
import org.simpleflatmapper.reflect.meta.MapClassMeta;
import org.simpleflatmapper.reflect.meta.ObjectClassMeta;
import org.simpleflatmapper.reflect.meta.OptionalClassMeta;
import org.simpleflatmapper.reflect.meta.PassThroughClassMeta;
import org.simpleflatmapper.reflect.meta.TupleClassMeta;
import org.simpleflatmapper.util.BiConsumer;
import org.simpleflatmapper.util.Consumer;
import org.simpleflatmapper.util.ProducerServiceLoader;
import org.simpleflatmapper.util.TupleHelper;
import org.simpleflatmapper.util.TypeHelper;
import org.simpleflatmapper.util.UnaryFactory;

/**
 * ReflectionService used in Curiostack
 *
 * <p>Most of the content is the same as DefaultReflectionService, with the change to use
 * org.ow2.asm instead of org.simpleflatmapper.ow2ams.
 */
public class CurioReflectionService extends ReflectionService {

  private static final UnaryFactory<ReflectionService, ClassMeta<?>>[] predefined =
      getPredifinedClassMetaFactory();
  private static final Consumer<BiConsumer<String, UnaryFactory<Type, Member>>>[]
      predefinedBuilderProducers = getPredifinedBuilderProducers();

  private final AliasProvider aliasProvider;
  private final boolean builderIgnoresNullValues;

  private final ConcurrentMap<Type, ClassMeta<?>> metaCache = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, UnaryFactory<Type, Member>> builderMethods =
      new ConcurrentHashMap<>();
  private final Map<ClassLoader, AsmFactory> asmFactoryPerClassLoader;
  private final boolean isAsmActivated;

  public CurioReflectionService(final AsmFactory asmFactory) {
    this(
        asmFactory != null,
        defaultAsmFactortyPerClassLoader(asmFactory),
        AliasProviderService.getAliasProvider(),
        true);
  }

  private CurioReflectionService(
      boolean isAsmActivated,
      Map<ClassLoader, AsmFactory> asmFactoryPerClassLoader,
      AliasProvider aliasProvider,
      boolean builderIgnoresNullValues) {

    this.isAsmActivated = isAsmActivated;
    this.asmFactoryPerClassLoader = asmFactoryPerClassLoader;
    this.aliasProvider = aliasProvider;
    this.builderIgnoresNullValues = builderIgnoresNullValues;
    initPredefined();
  }

  private static Map<ClassLoader, AsmFactory> defaultAsmFactortyPerClassLoader(
      AsmFactory asmFactory) {

    Map<ClassLoader, AsmFactory> map = new HashMap<>();
    if (asmFactory != null) map.put(asmFactory.targetClassLoader, asmFactory);
    return map;
  }

  private void initPredefined() {
    for (UnaryFactory<ReflectionService, ClassMeta<?>> factory : predefined) {
      ClassMeta<?> classMeta = factory.newInstance(this);
      metaCache.put(classMeta.getType(), classMeta);
    }
    for (Consumer<BiConsumer<String, UnaryFactory<Type, Member>>> factory :
        predefinedBuilderProducers) {
      factory.accept(builderMethods::put);
    }
  }

  @Override
  public void registerClassMeta(Type type, ClassMeta<?> classMeta) {
    metaCache.put(type, classMeta);
  }

  @Override
  public ObjectSetterFactory getObjectSetterFactory() {
    return new ObjectSetterFactory(this);
  }

  @Override
  public InstantiatorFactory getInstantiatorFactory() {
    return new InstantiatorFactory(this);
  }

  @Override
  public boolean isAsmActivated() {
    return isAsmActivated;
  }

  @Override
  public AsmFactory getAsmFactory(ClassLoader classLoader) {
    if (classLoader == null) // system class loader override with sfm one
    classLoader = getClass().getClassLoader();

    if (!isAsmActivated) return null;
    synchronized (asmFactoryPerClassLoader) {
      AsmFactory asmFactory = asmFactoryPerClassLoader.get(classLoader);
      if (asmFactory == null) {
        asmFactory = new AsmFactory(classLoader);
        asmFactoryPerClassLoader.put(classLoader, asmFactory);
      }
      return asmFactory;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> ClassMeta<T> getClassMeta(Type target) {
    requireNonNull("target", target);
    ClassMeta<T> meta = (ClassMeta<T>) metaCache.get(target);
    if (meta == null) {
      meta = newClassMeta(target);
      requireNonNull("meta", meta);
      metaCache.putIfAbsent(target, meta);
    }
    return meta;
  }

  @SuppressWarnings("unchecked")
  private <T> ClassMeta<T> newClassMeta(Type target) {

    Class<T> clazz = TypeHelper.toClass(target);

    if (target instanceof WildcardType) {
      Type[] upperBounds = ((WildcardType) target).getUpperBounds();
      if (upperBounds.length == 1) {
        target = upperBounds[0];
      }
    }
    if (clazz.isArray()) {
      return newArrayMeta(clazz);
    } else if (Optional.class.isAssignableFrom(clazz)) {
      return new OptionalClassMeta(target, this);
    } else if (clazz.isAnnotationPresent(PassThrough.class)) {
      return new PassThroughClassMeta(target, this);
    } else if (TupleHelper.isTuple(target)) {
      return new TupleClassMeta<>(target, this);
    } else if (isFastTuple(clazz)) {
      return new FastTupleClassMeta<>(target, this);
    } else if (Map.class.isAssignableFrom(clazz)) {
      return (ClassMeta<T>) newMapMeta(target);
    } else if (ArrayClassMeta.supports(target)) {
      return newCollectionMeta(target);
    }

    if (isAbstractOrInterface(target)) {
      target = findImplementation(target);
    }

    return new ObjectClassMeta<>(target, getBuilderInstantiator(target), this);
  }

  private static boolean isAbstractOrInterface(Type target) {
    Class<?> clazz = TypeHelper.toClass(target);
    return clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers());
  }

  private static Type findImplementation(Type target) {
    Class<Object> clazz = TypeHelper.toClass(target);
    Type implementation;

    implementation = ImmutableOrgHelper.findImplementation(clazz);

    if (implementation != null) return implementation;

    return target;
  }

  private Member getBuilderInstantiator(Type target) {
    String typeName = TypeHelper.toClass(target).getName();

    UnaryFactory<Type, Member> builderSupplier = builderMethods.get(typeName);

    if (builderSupplier != null) {
      return builderSupplier.newInstance(target);
    }

    return null;
  }

  @Override
  public <T> ClassMeta<T> getClassMetaExtraInstantiator(Type target, Member builderInstantiator) {
    return new ObjectClassMeta<>(target, builderInstantiator, this);
  }

  private <K, V> ClassMeta<Map<K, V>> newMapMeta(Type type) {
    TypeHelper.MapEntryTypes types = TypeHelper.getKeyValueTypeOfMap(type);
    return new MapClassMeta<>(type, types.getKeyType(), types.getValueType(), this);
  }

  private <T, E> ClassMeta<T> newArrayMeta(Class<T> clazz) {
    return ArrayClassMeta.<T, E>of(clazz, clazz.getComponentType(), this);
  }

  private <T, E> ClassMeta<T> newCollectionMeta(Type type) {
    return ArrayClassMeta.<T, E>of(type, TypeHelper.getComponentTypeOfListOrArray(type), this);
  }

  private static <T> boolean isFastTuple(Class<T> clazz) {
    Class<?> superClass = clazz.getSuperclass();
    return superClass != null && "com.boundary.tuple.FastTuple".equals(superClass.getName());
  }

  @Override
  public String getColumnName(Method method) {
    return aliasProvider.getAliasForMethod(method);
  }

  @Override
  public String getColumnName(Field field) {
    return aliasProvider.getAliasForField(field);
  }

  @Override
  public List<InstantiatorDefinition> extractInstantiator(Type target, Member extraInstantiator)
      throws IOException {
    List<InstantiatorDefinition> list;

    if (!ReflectionInstantiatorDefinitionFactory.areParameterNamePresent(target)) {
      try {
        list = AsmInstantiatorDefinitionFactory.extractDefinitions(target);
      } catch (IllegalArgumentException e) {
        // byte code version issue
        list = ReflectionInstantiatorDefinitionFactory.extractDefinitions(target);
      } catch (IOException e) {
        // no access to class file
        list = ReflectionInstantiatorDefinitionFactory.extractDefinitions(target);
      }
    } else {
      list = ReflectionInstantiatorDefinitionFactory.extractDefinitions(target);
    }

    if (TypeHelper.isKotlinClass(target)) {
      kotlinReducationForDefaultValue(list);
    }

    if (extraInstantiator == null) {
      list.addAll(BuilderInstantiatorDefinitionFactory.extractDefinitions(target));

      if (list.isEmpty()) {
        Class<?> enclosing = TypeHelper.toClass(target).getEnclosingClass();
        while (enclosing != null) {
          for (Method m : enclosing.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers())
                && Modifier.isStatic(m.getModifiers())
                && TypeHelper.toClass(target).isAssignableFrom(m.getReturnType())) {
              list.add(ReflectionInstantiatorDefinitionFactory.definition(m));
            }
          }
          enclosing = enclosing.getEnclosingClass();
        }
      }
    } else {
      if (extraInstantiator instanceof Method
          && TypeHelper.areEquals(target, ((Method) extraInstantiator).getGenericReturnType())) {
        // factory method
        list.add(ReflectionInstantiatorDefinitionFactory.definition(((Method) extraInstantiator)));
      } else {
        final BuilderInstantiatorDefinition builder =
            BuilderInstantiatorDefinitionFactory.getDefinitionForBuilder(extraInstantiator, target);
        if (builder == null) {
          throw new IllegalArgumentException(
              "Could not find any setters or build method on builder " + extraInstantiator);
        }
        list.add(builder);
      }
    }

    list.sort(InstantiatorDefinitions.COMPARATOR);

    return list;
  }

  private static void kotlinReducationForDefaultValue(List<InstantiatorDefinition> list) {

    // look for potential kotlin default value
    List<ExecutableInstantiatorDefinition> potentialKotlinDefaultValue =
        kotlingDefaultValueConstructor(list);

    if (potentialKotlinDefaultValue.isEmpty()) return;

    // remove them from original list
    list.removeAll(potentialKotlinDefaultValue);

    // match them to non default constructor
    for (ExecutableInstantiatorDefinition def : potentialKotlinDefaultValue) {
      for (int j = 0; j < list.size(); j++) {
        InstantiatorDefinition id = list.get(j);

        if (isKotlinOriginalConstructor(def, id)) {
          list.set(
              j,
              new KotlinDefaultConstructorInstantiatorDefinition(
                  (ExecutableInstantiatorDefinition) id, def));
          break;
        }
      }
    }
  }

  private static List<ExecutableInstantiatorDefinition> kotlingDefaultValueConstructor(
      List<InstantiatorDefinition> list) {
    List<ExecutableInstantiatorDefinition> potentialKotlinDefaultValue = new ArrayList<>();

    for (InstantiatorDefinition id : list) {
      if (id instanceof ExecutableInstantiatorDefinition
          && ((ExecutableInstantiatorDefinition) id).getExecutable() instanceof Constructor) {
        Constructor c = (Constructor) ((ExecutableInstantiatorDefinition) id).getExecutable();
        if (c.isSynthetic()) {
          Class[] parameterTypes = c.getParameterTypes();
          if (parameterTypes[parameterTypes.length - 1]
              .getName()
              .equals("kotlin.jvm.internal.DefaultConstructorMarker")) {
            // got one
            potentialKotlinDefaultValue.add((ExecutableInstantiatorDefinition) id);
          }
        }
      }
    }
    return potentialKotlinDefaultValue;
  }

  private static boolean isKotlinOriginalConstructor(
      ExecutableInstantiatorDefinition def, InstantiatorDefinition id) {
    if (id instanceof ExecutableInstantiatorDefinition) {
      ExecutableInstantiatorDefinition eid = (ExecutableInstantiatorDefinition) id;
      if (eid.getExecutable() instanceof Constructor) {
        int nbParams = eid.getParameters().length;
        int syntheticParameters = (nbParams / Integer.SIZE) + 1 + /* DefaultConstructorMarker */ 1;

        if (nbParams + syntheticParameters != def.getParameters().length) {
          return false;
        }

        for (int i = 0; i < nbParams; i++) {
          if (!def.getParameters()[i].getType().equals(id.getParameters()[i].getType())) {
            return false;
          }
        }

        for (int i = nbParams; i < nbParams + syntheticParameters - 1; i++) {
          if (!def.getParameters()[i].getType().equals(int.class)) {
            return false;
          }
        }

        return true;
      }
    }
    return false;
  }

  @Override
  public ObjectGetterFactory getObjectGetterFactory() {
    return new ObjectGetterFactory(this);
  }

  @Override
  public CurioReflectionService withAliasProvider(AliasProvider aliasProvider) {
    return new CurioReflectionService(
        isAsmActivated, asmFactoryPerClassLoader,
        aliasProvider, builderIgnoresNullValues);
  }

  @Override
  public CurioReflectionService withBuilderIgnoresNullValues(boolean builderIgnoresNullValues) {
    return new CurioReflectionService(
        isAsmActivated, asmFactoryPerClassLoader,
        aliasProvider, builderIgnoresNullValues);
  }

  /** No effect anymore */
  @Override
  @Deprecated
  @SuppressWarnings("InlineMeSuggester")
  public CurioReflectionService withSelfScoreFullName(boolean selfScoreFullName) {
    return this;
  }

  @Override
  public boolean builderIgnoresNullValues() {
    return builderIgnoresNullValues;
  }

  @Override
  @Deprecated
  @SuppressWarnings("InlineMeSuggester")
  public boolean selfScoreFullName() {
    return false;
  }

  @Override
  public void registerBuilder(String name, DefaultBuilderSupplier defaultBuilderSupplier) {
    builderMethods.put(name, defaultBuilderSupplier);
  }

  private static Consumer<BiConsumer<String, UnaryFactory<Type, Member>>>[]
      getPredifinedBuilderProducers() {
    final List<Consumer<BiConsumer<String, UnaryFactory<Type, Member>>>> list = new ArrayList<>();
    Consumer<Consumer<BiConsumer<String, UnaryFactory<Type, Member>>>> consumer = list::add;

    ProducerServiceLoader.produceFromServiceLoader(
        ServiceLoader.load(BuilderProducer.class), consumer);

    consumer.accept(
        biConsumer ->
            biConsumer.accept(
                "javax.money.MonetaryAmount",
                new DefaultBuilderSupplier("javax.money.Monetary", "getDefaultAmountFactory")));

    return list.toArray(new Consumer[0]);
  }

  @SuppressWarnings("unchecked")
  private static UnaryFactory<ReflectionService, ClassMeta<?>>[] getPredifinedClassMetaFactory() {
    final List<UnaryFactory<ReflectionService, ClassMeta<?>>> list = new ArrayList<>();
    Consumer<UnaryFactory<ReflectionService, ClassMeta<?>>> consumer = list::add;

    new JavaLangClassMetaFactoryProducer().produce(consumer);

    ProducerServiceLoader.produceFromServiceLoader(
        ServiceLoader.load(ClassMetaFactoryProducer.class), consumer);

    return list.toArray(new UnaryFactory[0]);
  }

  private static final AsmFactory _defaultAsmFactory =
      new AsmFactory(CurioReflectionService.class.getClassLoader());

  public static ReflectionService newInstance() {
    return newInstance(true);
  }

  public static CurioReflectionService newInstance(boolean useAsmGeneration) {
    return new CurioReflectionService(
        useAsmGeneration && canSeeSetterFromContextClassLoader() ? _defaultAsmFactory : null);
  }

  private static boolean canSeeSetterFromContextClassLoader() {
    try {
      Class.forName(Setter.class.getName(), false, Thread.currentThread().getContextClassLoader());
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
