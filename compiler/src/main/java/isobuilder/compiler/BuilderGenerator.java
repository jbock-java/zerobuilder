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

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;

import static com.google.auto.common.MoreElements.asType;
import static com.google.auto.common.MoreTypes.asDeclared;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.*;

final class BuilderGenerator extends SourceFileGenerator<ExecutableElement> {

  @Inject
  BuilderGenerator(Filer filer, Elements elements) {
    super(filer, elements);
  }

  @Override
  ClassName nameGeneratedType(ExecutableElement method) {
    DeclaredType returnType = asDeclared(method.getReturnType());
    TypeElement typeElement = asType(returnType.asElement());
    ClassName className = ClassName.get(typeElement);
    String name = Joiner.on('_').join(className.simpleNames()) + "Builder";
    return className.topLevelClassName().peerClass(name);
  }

  @Override
  Optional<? extends Element> getElementForErrorReporting(ExecutableElement input) {
    return Optional.absent();
  }

  @Override
  Optional<TypeSpec.Builder> write(
      ClassName generatedTypeName, ExecutableElement method) {
    TypeSpec.Builder builderBuilder =
        classBuilder(generatedTypeName).addModifiers(PUBLIC, FINAL);
    builderBuilder.addMethod(constructorBuilder().addModifiers(PRIVATE).build());
    return Optional.of(builderBuilder);
  }

}
