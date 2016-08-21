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

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

import javax.annotation.processing.Filer;
import javax.inject.Inject;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;

import java.util.List;

import static com.google.auto.common.MoreElements.asType;
import static com.google.auto.common.MoreTypes.asDeclared;
import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.*;

final class ContractGenerator extends SourceFileGenerator<ExecutableElement> {

  @Inject
  ContractGenerator(Filer filer, Elements elements) {
    super(filer, elements);
  }

  @Override
  ClassName nameGeneratedType(ExecutableElement method) {
    ClassName className = returnTypeName(method);
    String name = Joiner.on('_').join(className.simpleNames()) + "BuilderContract";
    return className.topLevelClassName().peerClass(name);
  }

  private ClassName returnTypeName(ExecutableElement method) {
    DeclaredType returnType = asDeclared(method.getReturnType());
    TypeElement typeElement = asType(returnType.asElement());
    return ClassName.get(typeElement);
  }

  @Override
  Optional<? extends Element> getElementForErrorReporting(ExecutableElement input) {
    return Optional.absent();
  }

  @Override
  Optional<Builder> write(
      ClassName generatedTypeName, ExecutableElement exe) {
    Builder contract = classBuilder(generatedTypeName).addModifiers(PUBLIC, FINAL);
    contract.addType(updaterSpec(generatedTypeName, exe));

//    List<? extends VariableElement> args = method.getParameters();
//    for (int i = 0; i < args.size(); i++) {
//      VariableElement arg = args.get(i);
//      Builder step = interfaceBuilder(arg.getSimpleName() + "Step");
//      MethodSpec.Builder stepMethod = methodBuilder(arg.getSimpleName().toString());
//      step.addMethod(stepMethod.build());
//      TypeSpec stepSpec = step.build();
//      stepMethod.returns(TypeName.get(stepSpec))
//      contract.addType(stepSpec);
//    }

    contract.addMethod(constructorBuilder().addModifiers(PRIVATE).build());
    return Optional.of(contract);
  }


  private TypeSpec updaterSpec(ClassName generatedTypeName, ExecutableElement exe) {
    String updaterSimpleName = returnTypeName(exe).simpleName() + "Updater";
    Builder updater = interfaceBuilder(updaterSimpleName);
    for (VariableElement arg : exe.getParameters()) {
      String name = "update" + LOWER_CAMEL.to(UPPER_CAMEL, arg.toString());
      MethodSpec method = methodBuilder(name)
          .returns(generatedTypeName.nestedClass(updaterSimpleName))
          .addParameter(TypeName.get(arg.asType()), arg.getSimpleName().toString())
          .addModifiers(PUBLIC, ABSTRACT)
          .build();
      updater.addMethod(method);
    }
    return updater.build();
  }

}
