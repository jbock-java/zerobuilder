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
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.*;

final class MyGenerator extends SourceFileGenerator<Target> {

  @Inject
  MyGenerator(Filer filer, Elements elements) {
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
    StepSpec firstStep = target.stepSpecs().get(0);
    StepSpec secondStep = target.stepSpecs().get(1);
    ParameterSpec parameter = firstStep.asParameter();
    TypeSpec.Builder builder = classBuilder(generatedClassName)
        .addMethod(methodBuilder(firstStep.argument().getSimpleName().toString())
            .returns(secondStep.stepName())
            .addParameter(parameter)
            .addStatement("return new $T($N)", target.implName(), parameter.name)
            .addModifiers(PUBLIC, STATIC)
            .build());
    return Optional.of(builder
        .addType(buildImpl(target))
        .addType(buildContract(target))
        .addModifiers(PUBLIC, FINAL)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build()));
  }

  private static TypeSpec buildImpl(Target target) {
    Target.Impl impl = target.impl();
    return classBuilder(target.implName())
        .addSuperinterfaces(target.contractInterfaceNames())
        .addFields(impl.fields())
        .addMethod(impl.constructor())
        .addMethods(impl.updaters())
        .addMethods(impl.steppers())
        .addMethod(impl.buildMethod())
        .addModifiers(FINAL, STATIC)
        .build();
  }

  private static TypeSpec buildContract(Target target) {
    Target.Contract contract = target.contract();
    return classBuilder(target.contractName())
        .addTypes(contract.contractInterfaces())
        .addModifiers(PUBLIC, FINAL, STATIC)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

}
