/*
 * Copyright (C) 2014 The Dagger Authors.
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

package isobuilder.compiler;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;

import static com.google.auto.common.MoreElements.asType;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static isobuilder.compiler.Util.upcase;
import static javax.lang.model.element.Modifier.*;

final class ContractGenerator extends SourceFileGenerator<Target> {

  @Inject
  ContractGenerator(Filer filer, Elements elements) {
    super(filer, elements);
  }

  @Override
  ClassName nameGeneratedType(Target target) {
    return target.generatedClassName();
  }

  @Override
  Optional<? extends Element> getElementForErrorReporting(Target input) {
    return Optional.absent();
  }

  @Override
  Optional<TypeSpec.Builder> write(
      ClassName generatedClassName, Target target) {
    TypeSpec.Builder builder = classBuilder(generatedClassName);
    if (!target.stepSpecs().isEmpty()) {
      builder.addMethod(methodBuilder("builder")
          .returns(target.stepSpecs().get(0).stepName())
          .addStatement("return new $T()", target.implName())
          .addModifiers(PUBLIC, STATIC)
          .build());
    }
    return Optional.of(builder
        .addType(buildImpl(target))
        .addType(buildContract(target))
        .addModifiers(PUBLIC, FINAL)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build()));
  }

  private static TypeSpec buildImpl(Target target) {
    TypeSpec.Builder builder = classBuilder(target.implName());
    for (StepSpec stepSpec : target.stepSpecs()) {
      String name = stepSpec.argument().getSimpleName().toString();
      builder.addField(TypeName.get(stepSpec.argument().asType()), name, PRIVATE);
    }
    for (StepSpec stepSpec : target.stepSpecs()) {
      String name = stepSpec.argument().getSimpleName().toString();
      builder.addMethod(methodBuilder("update" + upcase(name))
          .addAnnotation(Override.class)
          .returns(target.updaterName())
          .addParameter(TypeName.get(stepSpec.argument().asType()), name)
          .addStatement("this.$N = $N", name, name)
          .addStatement("return this")
          .addModifiers(PUBLIC)
          .build());
    }
    for (StepSpec stepSpec : target.stepSpecs()) {
      String name = stepSpec.argument().getSimpleName().toString();
      builder.addMethod(methodBuilder(name)
          .addAnnotation(Override.class)
          .returns(stepSpec.returnType())
          .addParameter(TypeName.get(stepSpec.argument().asType()), name)
          .addStatement("this.$N = $N", name, name)
          .addStatement("return this")
          .addModifiers(PUBLIC)
          .build());
    }
    return builder.addSuperinterfaces(target.contractInterfaceNames())
        .addModifiers(FINAL, STATIC)
        .addMethod(methodBuilder("build")
            .addAnnotation(Override.class)
            .returns(TypeName.get(target.executableElement().getReturnType()))
//            .addStatement("return null")
            .addStatement("return $T.$N($L)",
                ClassName.get(asType(target.executableElement().getEnclosingElement())),
                target.executableElement().getSimpleName(),
                target.factoryCallArgs())
            .addModifiers(PUBLIC)
            .build())
        .build();
  }

  private static TypeSpec buildContract(Target target) {
    return classBuilder(target.contractName())
        .addTypes(target.contractInterfaces())
        .addModifiers(PUBLIC, FINAL, STATIC)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

}
