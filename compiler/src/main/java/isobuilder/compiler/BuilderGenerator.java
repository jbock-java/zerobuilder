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
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.*;

final class BuilderGenerator extends SourceFileGenerator<BuilderInfo> {

  BuilderGenerator(Filer filer, Elements elements) {
    super(filer, elements);
  }

  @Override
  ClassName nameGeneratedType(BuilderInfo input) {
    ClassName className = ClassName.get(input.sourceType());
    String name = Joiner.on('_').join(className.simpleNames()) + "Builder";
    return className.topLevelClassName().peerClass(name);
  }

  @Override
  Optional<? extends Element> getElementForErrorReporting(BuilderInfo input) {
    return Optional.absent();
  }

  @Override
  Optional<TypeSpec.Builder> write(
      ClassName generatedTypeName, BuilderInfo input) {
    TypeSpec.Builder mapKeyCreatorBuilder =
        classBuilder(generatedTypeName).addModifiers(PUBLIC, FINAL);
    mapKeyCreatorBuilder.addMethod(constructorBuilder().addModifiers(PRIVATE).build());
    return Optional.of(mapKeyCreatorBuilder);
  }

}
