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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.util.List;
import java.util.Stack;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
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
    return Optional.of(classBuilder(generatedClassName)
        .addType(buildContract(target))
        .addModifiers(PUBLIC, FINAL)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build()));
  }

  private static TypeSpec buildContract(Target target) {
    return classBuilder(target.contractName())
        .addTypes(contractInterfaces(target))
        .addModifiers(PUBLIC, FINAL, STATIC)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

  private static List<TypeSpec> contractInterfaces(Target target) {
    Stack<TypeSpec> specs = new Stack<>();
    ClassName updaterName = target.contractName().nestedClass(target.returnTypeName().simpleName() + "Updater");
    specs.push(updaterSpec(updaterName, target));
    for (int i = target.stepSpecs().size() - 1; i >= 0; i--) {
      ClassName stepName = target.stepSpec(i).getStepName();
      VariableElement argument = target.stepSpec(i).getArgument();
      TypeSpec stepSpec = stepSpec(stepName, argument, target.contractName().nestedClass(specs.peek().name));
      specs.push(stepSpec);
    }
    return specs;
  }

  private static TypeSpec stepSpec(ClassName stepName, VariableElement arg, ClassName returnType) {
    MethodSpec methodSpec = methodBuilder(arg.getSimpleName().toString())
        .returns(returnType)
        .addParameter(TypeName.get(arg.asType()), arg.getSimpleName().toString())
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
    return interfaceBuilder(stepName)
        .addMethod(methodSpec)
        .build();
  }

  private static TypeSpec updaterSpec(ClassName updaterClassName, Target target) {
    TypeSpec.Builder updater = interfaceBuilder(updaterClassName);
    for (VariableElement arg : target.executableElement().getParameters()) {
      updater.addMethod(methodBuilder("update" + upcase(arg.getSimpleName().toString()))
          .returns(updaterClassName)
          .addParameter(TypeName.get(arg.asType()), arg.getSimpleName().toString())
          .addModifiers(PUBLIC, ABSTRACT)
          .build());
    }
    return updater.build();
  }

}
