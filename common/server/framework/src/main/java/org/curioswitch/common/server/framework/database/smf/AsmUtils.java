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

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.LSTORE;
import static org.objectweb.asm.Opcodes.SIPUSH;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.simpleflatmapper.reflect.ParameterizedTypeImpl;
import org.simpleflatmapper.util.Consumer;
import org.simpleflatmapper.util.ErrorHelper;
import org.simpleflatmapper.util.Predicate;
import org.simpleflatmapper.util.TypeHelper;

/**
 * AsmUtils used in Curiostack
 *
 * <p>Most of the content is the same as org.simpleflatmapper.reflect.asm.AsmUtils, with the change
 * to use org.ow2.asm instead of org.simpleflatmapper.ow2ams.
 */
public class AsmUtils {

  public static final String ASM_DUMP_TARGET_DIR = "asm.dump.target.dir";

  public static final Type[] EMPTY_TYPE_ARRAY = new Type[0];

  public static final int API = Opcodes.ASM9;

  static final Map<Class<?>, Class<?>> wrappers = new HashMap<>();

  static {
    wrappers.put(boolean.class, Boolean.class);
    wrappers.put(byte.class, Byte.class);
    wrappers.put(char.class, Character.class);
    wrappers.put(double.class, Double.class);
    wrappers.put(float.class, Float.class);
    wrappers.put(int.class, Integer.class);
    wrappers.put(long.class, Long.class);
    wrappers.put(short.class, Short.class);
    wrappers.put(void.class, Void.class);
  }

  static final Map<Class<?>, String> primitivesType = new HashMap<>();

  static {
    primitivesType.put(boolean.class, "Z");
    primitivesType.put(byte.class, "B");
    primitivesType.put(char.class, "C");
    primitivesType.put(double.class, "D");
    primitivesType.put(float.class, "F");
    primitivesType.put(int.class, "I");
    primitivesType.put(long.class, "J");
    primitivesType.put(short.class, "S");
    primitivesType.put(void.class, "V");
  }

  static final Map<String, String> stringToPrimitivesType = new HashMap<>();

  static {
    stringToPrimitivesType.put("Boolean", "Z");
    stringToPrimitivesType.put("Byte", "B");
    stringToPrimitivesType.put("Character", "C");
    stringToPrimitivesType.put("Double", "D");
    stringToPrimitivesType.put("Float", "F");
    stringToPrimitivesType.put("Int", "I");
    stringToPrimitivesType.put("Long", "J");
    stringToPrimitivesType.put("Short", "S");
  }

  static final Map<Class<?>, Integer> loadOps = new HashMap<>();

  static {
    loadOps.put(boolean.class, ILOAD);
    loadOps.put(byte.class, ILOAD);
    loadOps.put(char.class, ILOAD);
    loadOps.put(double.class, DLOAD);
    loadOps.put(float.class, FLOAD);
    loadOps.put(int.class, ILOAD);
    loadOps.put(long.class, LLOAD);
    loadOps.put(short.class, ILOAD);
  }

  static final Map<Class<?>, Integer> storeOps = new HashMap<>();

  static {
    storeOps.put(boolean.class, ISTORE);
    storeOps.put(byte.class, ISTORE);
    storeOps.put(char.class, ISTORE);
    storeOps.put(double.class, DSTORE);
    storeOps.put(float.class, FSTORE);
    storeOps.put(int.class, ISTORE);
    storeOps.put(long.class, LSTORE);
    storeOps.put(short.class, ISTORE);
  }

  static final Map<Class<?>, Integer> returnOps = new HashMap<>();

  static {
    returnOps.put(boolean.class, IRETURN);
    returnOps.put(byte.class, IRETURN);
    returnOps.put(char.class, IRETURN);
    returnOps.put(double.class, DRETURN);
    returnOps.put(float.class, FRETURN);
    returnOps.put(int.class, IRETURN);
    returnOps.put(long.class, LRETURN);
    returnOps.put(short.class, IRETURN);
  }

  static final Map<Class<?>, Integer> defaultValue = new HashMap<>();

  static {
    defaultValue.put(boolean.class, ICONST_0);
    defaultValue.put(byte.class, ICONST_0);
    defaultValue.put(char.class, ICONST_0);
    defaultValue.put(double.class, DCONST_0);
    defaultValue.put(float.class, FCONST_0);
    defaultValue.put(int.class, ICONST_0);
    defaultValue.put(long.class, LCONST_0);
    defaultValue.put(short.class, ICONST_0);
  }

  static final Set<Class<?>> primitivesClassAndWrapper = new HashSet<>();

  static {
    primitivesClassAndWrapper.addAll(wrappers.keySet());
    primitivesClassAndWrapper.addAll(wrappers.values());
  }

  @Nullable static File targetDir = null;

  static {
    String targetDirStr = System.getProperty(ASM_DUMP_TARGET_DIR);
    if (targetDirStr != null) {
      targetDir = new File(targetDirStr);
      targetDir.mkdirs();
    }
  }

  private AsmUtils() {}

  public static String toAsmType(final String name) {
    return name.replace('.', '/');
  }

  public static String toAsmType(final Type type) {
    if (TypeHelper.isPrimitive(type)) {
      return primitivesType.get(TypeHelper.toClass(type));
    }
    return toAsmType(TypeHelper.toClass(type).getName());
  }

  public static String toTargetTypeDeclaration(Type targetType) {
    if (TypeHelper.isPrimitive(targetType)) {
      return primitivesType.get(TypeHelper.toClass(targetType));
    }
    return toTargetTypeDeclaration(toAsmType(targetType));
  }

  public static String toTargetTypeDeclaration(String targetType) {
    if (targetType.startsWith("[")) {
      return targetType;
    } else {
      return "L" + targetType + ";";
    }
  }

  public static String toGenericAsmType(final Type type) {
    StringBuilder sb = new StringBuilder();

    sb.append(toAsmType(type));

    Type[] typeParameters = null;

    if (type instanceof ParameterizedType) {
      typeParameters = ((ParameterizedType) type).getActualTypeArguments();
    }

    if (typeParameters != null && typeParameters.length > 0) {
      sb.append("<");

      for (Type t : typeParameters) {
        sb.append(toTargetTypeDeclaration(toGenericAsmType(t)));
      }

      sb.append(">");
    }

    return sb.toString();
  }

  public static byte[] writeClassToFile(final String className, final byte[] bytes)
      throws IOException {
    return writeClassToFileInDir(className, bytes, targetDir);
  }

  public static byte[] writeClassToFileInDir(String className, byte[] bytes, File targetDir)
      throws IOException {
    if (targetDir != null) {
      _writeClassToFileInDir(className, bytes, targetDir);
    }
    return bytes;
  }

  private static void _writeClassToFileInDir(String className, byte[] bytes, File targetDir)
      throws IOException {
    final int lastIndex = className.lastIndexOf('.');
    final String filename = className.substring(lastIndex + 1) + ".class";
    final String directory = className.substring(0, lastIndex).replace('.', '/');
    final File packageDir = new File(targetDir, directory);
    packageDir.mkdirs();

    try (FileOutputStream fos = new FileOutputStream(new File(packageDir, filename))) {
      fos.write(bytes);
    }
  }

  public static Type toGenericType(String sig, List<String> genericTypeNames, Type target)
      throws ClassNotFoundException {
    ClassLoader classLoader =
        TypeHelper.getClassLoader(target, Thread.currentThread().getContextClassLoader());

    SignatureReader reader = new SignatureReader(sig);
    final List<Type> types = new ArrayList<>();
    TypeCreator typeCreator = new TypeCreator(types::add, classLoader, genericTypeNames, target) {};

    reader.accept(typeCreator);
    typeCreator.visitEnd();
    return types.get(0);
  }

  private static class TypeCreator extends SignatureVisitor {
    final Consumer<Type> consumer;
    final ClassLoader classLoader;
    private final List<String> genericTypeNames;
    private final Type target;

    protected Type type;
    final List<Type> arguments = new ArrayList<>();

    boolean flushed = false;

    public TypeCreator(
        Consumer<Type> consumer,
        ClassLoader classLoader,
        List<String> genericTypeNames,
        Type target) {
      super(API);
      this.consumer = consumer;
      this.classLoader = classLoader;
      this.genericTypeNames = genericTypeNames;
      this.target = target;
    }

    @Override
    public void visitFormalTypeParameter(String name) {
      super.visitFormalTypeParameter(name);
    }

    @Override
    public SignatureVisitor visitParameterType() {
      return super.visitParameterType();
    }

    @Override
    public void visitBaseType(char descriptor) {
      switch (descriptor) {
        case 'Z':
          type = boolean.class;
          break;
        case 'B':
          type = byte.class;
          break;
        case 'C':
          type = char.class;
          break;
        case 'D':
          type = double.class;
          break;
        case 'F':
          type = float.class;
          break;
        case 'I':
          type = int.class;
          break;
        case 'J':
          type = long.class;
          break;
        case 'S':
          type = short.class;
          break;
        default:
          throw new IllegalArgumentException("Unexpected primitiv " + descriptor);
      }
      visitEnd();
    }

    @Override
    public void visitTypeVariable(String name) {

      int i = genericTypeNames.indexOf(name);
      if (i >= 0 && target instanceof ParameterizedType) {
        Type resolvedType = ((ParameterizedType) target).getActualTypeArguments()[i];
        consumer.accept(resolvedType);
      }
    }

    @Override
    public SignatureVisitor visitArrayType() {
      return new TypeCreator(
          type -> consumer.accept(Array.newInstance(TypeHelper.toClass(type), 0).getClass()),
          classLoader,
          genericTypeNames,
          target);
    }

    @Override
    public void visitClassType(String name) {
      try {
        type = Class.forName(name.replace('/', '.'), true, classLoader);
      } catch (ClassNotFoundException e) {
        ErrorHelper.rethrow(e);
      }
    }

    @Override
    public SignatureVisitor visitTypeArgument(char wildcard) {
      return new TypeCreator(arguments::add, classLoader, genericTypeNames, target);
    }

    @Override
    public void visitEnd() {
      if (flushed) return;

      flushed = true;
      if (arguments.isEmpty()) {
        consumer.accept(type);
      } else {
        consumer.accept(
            new ParameterizedTypeImpl(
                TypeHelper.toClass(type), arguments.toArray(EMPTY_TYPE_ARRAY)));
      }
    }
  }

  public static Type findClosestPublicTypeExposing(Type type, Class<?> expose) {
    return findTypeInHierarchy(type, new TypeIsPublicAndImplement(expose));
  }

  public static Type findTypeInHierarchy(Type type, Predicate<Type> predicate) {

    if (predicate.test(type)) {
      return type;
    }

    // check interfaces
    Class<Object> targetClass = TypeHelper.toClass(type);

    for (Type i : targetClass.getGenericInterfaces()) {
      if (predicate.test(i)) {
        return i;
      }
    }

    Type st = targetClass.getGenericSuperclass();
    if (st != null) {
      return findTypeInHierarchy(st, predicate);
    }

    return null;
  }

  public static void invoke(MethodVisitor mv, Method method) {
    if (Modifier.isStatic(method.getModifiers())) {
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC,
          toAsmType(method.getDeclaringClass()),
          method.getName(),
          toSignature(method),
          false);
    } else {
      invoke(mv, method.getName(), method.getDeclaringClass(), toSignature(method));
    }
  }

  public static void invoke(MethodVisitor mv, String method, Type target, String sig) {
    Type publicClass = findTypeInHierarchy(target, new TypeIsPublicAndHasMethodMethod(method));
    boolean isInterface = TypeHelper.toClass(publicClass).isInterface();
    mv.visitMethodInsn(
        isInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL,
        toAsmType(publicClass),
        method,
        sig,
        isInterface);
  }

  public static Class<?> toWrapperClass(Type type) {
    final Class<?> clazz = TypeHelper.toClass(type);
    if (clazz.isPrimitive()) {
      return wrappers.get(clazz);
    } else return clazz;
  }

  public static String toWrapperType(Type type) {
    return toAsmType(toWrapperClass(type));
  }

  public static List<String> extractGenericTypeNames(String sig) {
    List<String> types = new ArrayList<>();

    boolean nameDetected = false;
    int currentStart = -1;
    for (int i = 0; i < sig.length(); i++) {
      char c = sig.charAt(i);
      switch (c) {
        case '<':
        case ';':
          if (!nameDetected) {
            nameDetected = true;
            currentStart = i + 1;
          }
          break;
        case ':':
          types.add(sig.substring(currentStart, i));
          nameDetected = false;
          break;
        default:
          break;
      }
    }

    return types;
  }

  public static List<String> extractTypeNamesFromSignature(String sig) {
    final List<String> types = new ArrayList<>();

    SignatureReader reader = new SignatureReader(sig);
    reader.accept(
        new SignatureVisitor(API) {

          // TypeSignature =
          // visitBaseType | visitTypeVariable | visitArrayType | ( visitClassType
          // visitTypeArgument* ( visitInnerClassType visitTypeArgument* )* visitEnd ) )

          class AppendType extends SignatureVisitor {
            StringBuilder sb = new StringBuilder();
            int l = 0;

            public AppendType() {
              super(API);
            }

            @Override
            public void visitBaseType(char descriptor) {
              if (descriptor != 'V') {
                sb.append(descriptor);

                if (l <= 0) {
                  flush();
                }
              }
            }

            @Override
            public void visitTypeVariable(String name) {
              sb.append("T");
              sb.append(name);
              sb.append(";");
              if (l <= 0) {
                flush();
              }
            }

            @Override
            public SignatureVisitor visitArrayType() {
              sb.append("[");
              return this;
            }

            @Override
            public void visitClassType(String name) {
              l++;
              sb.append("L");
              sb.append(name);
              sb.append("<");
            }

            @Override
            public void visitInnerClassType(String name) {
              visitClassType(name);
            }

            @Override
            public void visitTypeArgument() {}

            @Override
            public SignatureVisitor visitTypeArgument(char wildcard) {
              if (wildcard != '=') {
                sb.append(wildcard);
              }

              if (l <= 0) {
                flush();
              }
              return this;
            }

            @Override
            public void visitEnd() {
              l--;

              if (sb.charAt(sb.length() - 1) == '<') {
                sb.setLength(sb.length() - 1); // remove last char
              } else {
                sb.append('>');
              }

              sb.append(";");

              if (l <= 0) {
                flush();
              }
            }

            private void flush() {
              if (sb.length() > 0) {

                types.add(sb.toString());

                sb = new StringBuilder();
              }
            }
          }

          @Override
          public void visitFormalTypeParameter(String name) {}

          @Override
          public SignatureVisitor visitClassBound() {
            return super.visitInterfaceBound();
          }

          @Override
          public SignatureVisitor visitInterfaceBound() {
            return super.visitInterfaceBound();
          }

          @Override
          public SignatureVisitor visitParameterType() {
            return new AppendType();
          }

          @Override
          public SignatureVisitor visitReturnType() {
            return new AppendType();
          }

          @Override
          public SignatureVisitor visitExceptionType() {
            return new AppendType();
          }
        });

    return types;
  }

  public static String toSignature(Method exec) {
    StringBuilder sb = new StringBuilder();

    sb.append("(");
    for (Class<?> clazz : exec.getParameterTypes()) {
      sb.append(toTargetTypeDeclaration(clazz));
    }
    sb.append(")");

    sb.append(toTargetTypeDeclaration(exec.getReturnType()));

    return sb.toString();
  }

  public static int getLoadOps(Class<?> parameterType) {
    if (TypeHelper.isPrimitive(parameterType)) {
      return loadOps.get(parameterType);
    }
    return ALOAD;
  }

  public static int getStoreOps(Class<?> type) {
    if (TypeHelper.isPrimitive(type)) {
      return storeOps.get(type);
    }
    return ASTORE;
  }

  public static void addIndex(MethodVisitor mv, int i) {
    switch (i) {
      case 0:
        mv.visitInsn(ICONST_0);
        return;
      case 1:
        mv.visitInsn(ICONST_1);
        return;
      case 2:
        mv.visitInsn(ICONST_2);
        return;
      case 3:
        mv.visitInsn(ICONST_3);
        return;
      case 4:
        mv.visitInsn(ICONST_4);
        return;
      case 5:
        mv.visitInsn(ICONST_5);
        return;
      default:
        if (i <= Byte.MAX_VALUE) {
          mv.visitIntInsn(BIPUSH, i);
        } else if (i <= Short.MAX_VALUE) {
          mv.visitIntInsn(SIPUSH, i);
        } else {
          mv.visitLdcInsn(i);
        }
    }
  }

  private static class TypeIsPublicAndHasMethodMethod implements Predicate<Type> {
    private final String method;

    public TypeIsPublicAndHasMethodMethod(String method) {
      this.method = method;
    }

    @Override
    public boolean test(Type type) {
      Class<?> clazz = TypeHelper.toClass(type);
      if (!Modifier.isPublic(clazz.getModifiers())) {
        return false;
      }
      for (Method m : clazz.getMethods()) {
        if (m.getName().equals(method)) {
          return true;
        }
      }
      return false;
    }
  }

  private static class TypeIsPublicAndImplement implements Predicate<Type> {
    private final Class<?> expose;

    public TypeIsPublicAndImplement(Class<?> expose) {
      this.expose = expose;
    }

    @Override
    public boolean test(Type type) {
      Class<Object> targetClass = TypeHelper.toClass(type);
      return Modifier.isPublic(targetClass.getModifiers()) && expose.isAssignableFrom(targetClass);
    }
  }
}
