/*
 * Copyright 2010 Henry Coles
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.pitest.classinfo;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.pitest.bytecode.NullVisitor;
import org.pitest.coverage.codeassist.BridgeMethodFilter;
import org.pitest.coverage.codeassist.MethodFilteringAdapter;

public class ClassInfoVisitor extends MethodFilteringAdapter {

  private final ClassInfoBuilder classInfo;

  private ClassInfoVisitor(final ClassInfoBuilder classInfo,
      final ClassVisitor writer) {
    super(writer, BridgeMethodFilter.INSTANCE);
    this.classInfo = classInfo;
  }

  public final static ClassInfoBuilder getClassInfo(final String name,
      final byte[] bytes) {
    final ClassReader reader = new ClassReader(bytes);
    final ClassVisitor writer = new NullVisitor();

    final ClassInfoBuilder info = new ClassInfoBuilder();
    info.name = new ClassName(name);
    reader.accept(new ClassInfoVisitor(info, writer), 0);
    return info;
  }

  @Override
  public void visit(final int version, final int access, final String name,
      final String signature, final String superName, final String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    this.classInfo.access = access;
    this.classInfo.superClass = superName;
  }

  @Override
  public void visitOuterClass(final String owner, final String name,
      final String desc) {
    super.visitOuterClass(owner, name, desc);
    this.classInfo.outerClass = owner;
  }

  @Override
  public void visitInnerClass(final String name, final String outerName,
      final String innerName, final int access) {
    super.visitInnerClass(name, outerName, innerName, access);
    if ((outerName != null)
        && this.classInfo.name.equals(new ClassName(name))) {
      this.classInfo.outerClass = outerName;
    }
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String desc,
      final boolean visible) {
    final String type = desc.substring(1, desc.length() - 1);
    this.classInfo.registerAnnotation(type);
    return super.visitAnnotation(desc, visible);
  }

  @Override
  public MethodVisitor visitMethodIfRequired(final int access,
      final String name, final String desc, final String signature,
      final String[] exceptions, final MethodVisitor methodVisitor) {

    return new InfoMethodVisitor(this.classInfo, methodVisitor, name, desc);

  }

}

class InfoMethodVisitor extends MethodAdapter {
  private final ClassInfoBuilder classInfo;

  public InfoMethodVisitor(final ClassInfoBuilder classInfo,
      final MethodVisitor writer, final String name, final String methodDesc) {
    super(writer);
    this.classInfo = classInfo;
  }

  @Override
  public void visitLineNumber(final int line, final Label start) {

    this.classInfo.registerCodeLine(line);

  }

  @Override
  public AnnotationVisitor visitAnnotation(final String desc,
      final boolean visible) {
    final String type = desc.substring(1, desc.length() - 1);
    this.classInfo.registerAnnotation(type);
    return super.visitAnnotation(desc, visible);
  }

}
