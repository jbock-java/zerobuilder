package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;

final class GenericsImpl {

  private final ClassName impl;

  List<TypeSpec> stepImpls(ClassName implType,
                           List<TypeSpec> stepSpecs,
                           List<List<TypeVariableName>> methodParams,
                           List<List<TypeVariableName>> typeParams) {
    List<TypeSpec> builder = new ArrayList<>(stepSpecs.size());
    builder.addAll(nCopies(stepSpecs.size(), null));
    for (int i = 0; i < stepSpecs.size(); i++) {
      TypeSpec type = stepSpecs.get(i);
      MethodSpec method = type.methodSpecs.get(0);
      ParameterSpec parameter = method.parameters.get(0);
      List<FieldSpec> fields = fields(stepSpecs, i);
      builder.set(i, classBuilder(implType.nestedClass(type.name + "Impl"))
          .addFields(fields)
          .addMethod(createConstructor(fields))
          .addTypeVariables(typeParams.get(i))
          .addMethod(methodBuilder(method.name)
              .addParameter(parameter)
              .addTypeVariables(methodParams.get(i))
              .returns(method.returnType)
              .addCode(i == stepSpecs.size() - 1 ?
                  invoke(stepSpecs) :
                  statement("return new $T(this, $N)",
                      implType.nestedClass(stepSpecs.get(i + 1).name + "Impl"), parameter))
              .build())
          .build());
    }
    return builder;
  }

  static CodeBlock invoke(List<TypeSpec> stepSpecs) {
    return IntStream.range(0, stepSpecs.size())
        .mapToObj(i -> {
          CodeBlock.Builder block = CodeBlock.builder();
          for (int j = stepSpecs.size() - 3; j >= i; j--) {
            TypeSpec type = stepSpecs.get(j + 1);
            MethodSpec method = type.methodSpecs.get(0);
            ParameterSpec parameter = method.parameters.get(0);
            block.add("$N", parameter).add("Impl.");
          }
          TypeSpec type = stepSpecs.get(i);
          MethodSpec method = type.methodSpecs.get(0);
          ParameterSpec parameter = method.parameters.get(0);
          block.add("$N", parameter);
          return block.build();
        })
        .collect(joinCodeBlocks(", "));
  }

  private MethodSpec createConstructor(List<FieldSpec> fields) {
    List<ParameterSpec> parameters = transform(fields, field -> parameterSpec(field.type, field.name));
    return MethodSpec.constructorBuilder()
        .addParameters(parameters)
        .addCode(parameters.stream().map(parameter -> statement("this.$N = $N", parameter, parameter))
            .collect(joinCodeBlocks))
        .addModifiers(PRIVATE)
        .build();
  }

  private List<FieldSpec> fields(List<TypeSpec> stepSpecs, int i) {
    if (i == 0) {
      return emptyList();
    }
    if (i == 1) {
      return singletonList(parameterField(stepSpecs.get(0)));
    }
    TypeSpec previous = stepSpecs.get(i - 1);
    return asList(
        FieldSpec.builder(impl.nestedClass(previous.name), downcase(previous.name) + "Impl",
            PRIVATE, FINAL)
            .build(),
        parameterField(previous));
  }

  private FieldSpec parameterField(TypeSpec type) {
    MethodSpec method = type.methodSpecs.get(0);
    ParameterSpec parameter = method.parameters.get(0);
    return FieldSpec.builder(parameter.type, parameter.name, PRIVATE, FINAL)
        .build();
  }

  GenericsImpl(ClassName impl) {
    this.impl = impl;
  }
}
