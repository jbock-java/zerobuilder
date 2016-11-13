package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.NullPolicy.REJECT;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;
import static net.zerobuilder.compiler.generate.ZeroUtil.nullCheck;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;

final class GenericsImpl {

  private final ClassName impl;
  private final ClassName contract;
  private final SimpleStaticMethodGoalContext goal;

  List<TypeSpec> stepImpls(List<TypeSpec> stepSpecs,
                           List<List<TypeVariableName>> methodParams,
                           List<List<TypeVariableName>> typeParams) {
    List<TypeSpec> builder = new ArrayList<>(stepSpecs.size());
    builder.addAll(nCopies(stepSpecs.size(), null));
    for (int i = 0; i < stepSpecs.size(); i++) {
      TypeSpec stepSpec = stepSpecs.get(i);
      MethodSpec method = stepSpec.methodSpecs.get(0);
      ParameterSpec parameter = method.parameters.get(0);
      List<FieldSpec> fields = fields(stepSpecs, i, typeParams);
      TypeName superinterface = parameterizedTypeName(contract.nestedClass(stepSpec.name),
          stepSpec.typeVariables);
      builder.set(i, classBuilder(stepSpec.name + "Impl")
          .addFields(fields)
          .addSuperinterface(superinterface)
          .addMethod(createConstructor(fields))
          .addTypeVariables(typeParams.get(i))
          .addMethod(methodBuilder(method.name)
              .addAnnotation(Override.class)
              .addParameter(parameter)
              .addTypeVariables(methodParams.get(i))
              .addModifiers(PUBLIC)
              .returns(method.returnType)
              .addCode(getCodeBlock(stepSpecs, i, parameter))
              .build())
          .addModifiers(PRIVATE, STATIC, FINAL)
          .build());

    }
    return builder;
  }

  private CodeBlock getCodeBlock(List<TypeSpec> stepSpecs, int i, ParameterSpec parameter) {
    CodeBlock.Builder builder = CodeBlock.builder();
    if (goal.parameters.get(i).nullPolicy == REJECT
        && !goal.parameters.get(i).type.isPrimitive()) {
      builder.add(nullCheck(parameter.name, parameter.name));
    }
    if (i == stepSpecs.size() - 1) {
      return builder.add(fullInvoke(stepSpecs)).build();
    }
    ClassName next = impl.nestedClass(stepSpecs.get(i + 1).name + "Impl");
    return i == 0 && !goal.details.instance ?
        builder.addStatement("return new $T($N)", next, parameter).build() :
        builder.addStatement("return new $T(this, $N)", next, parameter).build();
  }

  private CodeBlock fullInvoke(List<TypeSpec> stepSpecs) {
    List<CodeBlock> blocks = basicInvoke(stepSpecs);
    CodeBlock invoke = goal.unshuffle(blocks)
        .stream()
        .collect(joinCodeBlocks(", "));
    return goal.details.instance ?
        statement("return $L.$L($L)",
            instance(stepSpecs),
            goal.details.methodName, invoke) :
        statement("return $T.$L($L)",
            goal.context.type,
            goal.details.methodName, invoke);
  }

  static CodeBlock instance(List<TypeSpec> stepSpecs) {
    CodeBlock.Builder builder = CodeBlock.builder();
    for (int i = stepSpecs.size() - 2; i >= 0; i--) {
      TypeSpec type = stepSpecs.get(i);
      builder.add("$L.", downcase(type.name) + "Impl");
    }
    return builder.add("instance").build();
  }

  static List<CodeBlock> basicInvoke(List<TypeSpec> stepSpecs) {
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
        .collect(toList());
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

  private List<FieldSpec> fields(List<TypeSpec> stepSpecs, int i, List<List<TypeVariableName>> typeParams) {
    if (i == 0) {
      return goal.details.instance ?
          singletonList(FieldSpec.builder(goal.context.type, "instance",
              PRIVATE, FINAL).build()) :
          emptyList();
    }
    if (i == 1 && !goal.details.instance) {
      return singletonList(parameterField(stepSpecs.get(0)));
    }
    TypeSpec stepSpec = stepSpecs.get(i - 1);
    TypeName implType = parameterizedTypeName(impl.nestedClass(stepSpec.name + "Impl"),
        typeParams.get(i - 1));
    return asList(
        FieldSpec.builder(implType, downcase(stepSpec.name) + "Impl",
            PRIVATE, FINAL)
            .build(),
        parameterField(stepSpec));
  }

  private FieldSpec parameterField(TypeSpec type) {
    MethodSpec method = type.methodSpecs.get(0);
    ParameterSpec parameter = method.parameters.get(0);
    return FieldSpec.builder(parameter.type, parameter.name, PRIVATE, FINAL)
        .build();
  }

  GenericsImpl(ClassName impl, ClassName contract, SimpleStaticMethodGoalContext goal) {
    this.impl = impl;
    this.contract = contract;
    this.goal = goal;
  }
}
