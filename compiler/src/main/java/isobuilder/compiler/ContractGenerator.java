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
    return target.nameGeneratedType("BuilderContract");
  }


  @Override
  Optional<? extends Element> getElementForErrorReporting(Target input) {
    return Optional.absent();
  }

  @Override
  Optional<TypeSpec.Builder> write(
      ClassName generatedTypeName, Target exe) {
    TypeSpec.Builder contract = classBuilder(generatedTypeName).addModifiers(PUBLIC, FINAL);
    List<? extends VariableElement> args = exe.getExecutableElement().getParameters();
    Stack<TypeSpec> specs = new Stack<>();
    specs.push(updaterSpec(generatedTypeName, exe));
    for (int i = args.size() - 1; i >= 0; i--) {
      specs.push(stepSpec(generatedTypeName, args.get(i), specs.peek()));
    }
    TypeSpec.Builder contractBuilder = interfaceBuilder(LOWER_CAMEL.to(UPPER_CAMEL, exe.returnTypeName().simpleName() + "Contract"));
    for (TypeSpec spec : specs) {
      contract.addType(spec);
      contractBuilder.addSuperinterface(generatedTypeName.nestedClass(spec.name));
    }
    contract.addType(contractBuilder.build());
    contract.addMethod(constructorBuilder().addModifiers(PRIVATE).build());
    return Optional.of(contract);
  }

  private TypeSpec stepSpec(ClassName generatedTypeName, VariableElement arg, TypeSpec returnType) {
    String stepSimpleName = LOWER_CAMEL.to(UPPER_CAMEL, arg.getSimpleName() + "Step");
    return interfaceBuilder(stepSimpleName)
        .addMethod(methodBuilder(arg.getSimpleName().toString())
            .returns(generatedTypeName.nestedClass(returnType.name))
            .addParameter(TypeName.get(arg.asType()), arg.getSimpleName().toString())
            .addModifiers(PUBLIC, ABSTRACT)
            .build())
        .build();
  }


  private TypeSpec updaterSpec(ClassName generatedTypeName, Target exe) {
    String updaterSimpleName = exe.returnTypeName().simpleName() + "Updater";
    TypeSpec.Builder updater = interfaceBuilder(updaterSimpleName);
    for (VariableElement arg : exe.getExecutableElement().getParameters()) {
      updater.addMethod(methodBuilder("update" + LOWER_CAMEL.to(UPPER_CAMEL, arg.getSimpleName().toString()))
          .returns(generatedTypeName.nestedClass(updaterSimpleName))
          .addParameter(TypeName.get(arg.asType()), arg.getSimpleName().toString())
          .addModifiers(PUBLIC, ABSTRACT)
          .build());
    }
    return updater.build();
  }

}
