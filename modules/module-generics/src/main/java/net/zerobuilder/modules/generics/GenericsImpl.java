package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.util.ArrayList;
import java.util.List;

import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Collections.nCopies;

final class GenericsImpl {

  static List<TypeSpec> stepImpls(ClassName implType, List<TypeSpec> stepSpecs,
                                  List<List<TypeVariableName>> methodParams) {
    List<TypeSpec> builder = new ArrayList<>(stepSpecs.size());
    builder.addAll(nCopies(stepSpecs.size(), null));
    for (int i = 0; i < stepSpecs.size(); i++) {
      TypeSpec type = stepSpecs.get(i);
      MethodSpec method = type.methodSpecs.get(0);
      ParameterSpec parameter = method.parameters.get(0);
      builder.set(i, classBuilder(implType.nestedClass(type.name + "Impl"))
          .addMethod(MethodSpec.methodBuilder(method.name)
              .addParameter(parameter)
              .addTypeVariables(methodParams.get(i))
              .returns(method.returnType)
              .build())
          .build());
    }
    return builder;
  }

  private GenericsImpl() {
    throw new UnsupportedOperationException("no instances");
  }
}
