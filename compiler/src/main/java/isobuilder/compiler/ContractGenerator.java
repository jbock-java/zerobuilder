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
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.util.List;
import java.util.Stack;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.*;

final class ContractGenerator extends SourceFileGenerator<Target> {

  @Inject
  ContractGenerator(Filer filer, Elements elements) {
    super(filer, elements);
  }

  @Override
  ClassName nameGeneratedType(Target target) {
    return target.contractName();
  }

  @Override
  Optional<? extends Element> getElementForErrorReporting(Target input) {
    return Optional.absent();
  }

  @Override
  Optional<TypeSpec.Builder> write(
      ClassName gen, Target target) {
    List<? extends VariableElement> args = target.getExecutableElement().getParameters();
    Stack<TypeSpec> specs = new Stack<>();
    ClassName updaterName = gen.nestedClass(target.returnTypeName().simpleName() + "Updater");
    specs.push(updaterSpec(updaterName, target));
    ImmutableList<StepSpec> stepSpecs = target.stepSpecs();
    for (int i = stepSpecs.size() - 1; i >= 0; i--) {
      ClassName stepName = stepSpecs.get(i).getStepName();
      VariableElement argument = stepSpecs.get(i).getArgument();
      TypeSpec stepSpec = stepSpec(stepName, argument, gen.nestedClass(specs.peek().name));
      specs.push(stepSpec);
    }
    ClassName contractName = gen.nestedClass(upcase(target.returnTypeName().simpleName() + "Contract"));
    TypeSpec.Builder contractBuilder = interfaceBuilder(contractName);
    for (TypeSpec spec : specs) {
      contractBuilder.addSuperinterface(gen.nestedClass(spec.name));
    }
    return Optional.of(classBuilder(gen)
        .addModifiers(PUBLIC, FINAL)
        .addTypes(specs)
        .addType(contractBuilder.build())
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build()));
  }

  private TypeSpec stepSpec(ClassName stepName, VariableElement arg, ClassName returnType) {
    return interfaceBuilder(stepName)
        .addMethod(methodBuilder(arg.getSimpleName().toString())
            .returns(returnType)
            .addParameter(TypeName.get(arg.asType()), arg.getSimpleName().toString())
            .addModifiers(PUBLIC, ABSTRACT)
            .build())
        .build();
  }


  private TypeSpec updaterSpec(ClassName updaterClassName, Target target) {
    TypeSpec.Builder updater = interfaceBuilder(updaterClassName);
    for (VariableElement arg : target.getExecutableElement().getParameters()) {
      updater.addMethod(methodBuilder("update" + upcase(arg.getSimpleName().toString()))
          .returns(updaterClassName)
          .addParameter(TypeName.get(arg.asType()), arg.getSimpleName().toString())
          .addModifiers(PUBLIC, ABSTRACT)
          .build());
    }
    return updater.build();
  }

  static String upcase(String s) {
    return LOWER_CAMEL.to(UPPER_CAMEL, s);
  }

}
