package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeName.VOID;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoGoalDetails.regularDetailsCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.generics.GenericsContract.nextStepType;

final class GenericsImpl {

  private final ClassName contract;
  private final SimpleRegularGoalDescription description;

  List<TypeSpec> stepImpls(List<List<TypeVariableName>> methodParams,
                           List<List<TypeVariableName>> typeParams) {
    List<TypeSpec> builder = new ArrayList<>(description.parameters.size());
    ImplFields implFields = new ImplFields(contract, description, typeParams);
    for (int i = 0; i < description.parameters.size(); i++) {
      builder.add(createStep(implFields, methodParams, typeParams, i));
    }
    return builder;
  }

  private TypeSpec createStep(ImplFields implFields,
                              List<List<TypeVariableName>> methodParams,
                              List<List<TypeVariableName>> typeParams, int i) {
    ParameterSpec parameter = parameterSpec(description.parameters.get(i).type, description.parameters.get(i).name);
    List<FieldSpec> fields = implFields.fields.apply(description.details, i);
    TypeSpec.Builder builder = classBuilder(upcase(description.parameters.get(i).name));
    builder.addMethod(createConstructor(fields));
    return builder.addFields(fields)
        .addTypeVariables(typeParams.get(i))
        .addMethod(methodBuilder(description.parameters.get(i).name)
            .addParameter(parameter)
            .addTypeVariables(methodParams.get(i))
            .addModifiers(PUBLIC)
            .returns(nextStepType(description, typeParams, i))
            .addCode(getCodeBlock(i, parameter))
            .addExceptions(i == description.parameters.size() - 1 ?
                description.thrownTypes :
                emptyList())
            .build())
        .addModifiers(PUBLIC, STATIC, FINAL)
        .build();
  }

  private CodeBlock getCodeBlock(int i, ParameterSpec parameter) {
    CodeBlock.Builder builder = CodeBlock.builder();
    if (i == description.parameters.size() - 1) {
      return builder.add(fullInvoke()).build();
    }
    ClassName next = contract.nestedClass(upcase(description.parameters.get(i + 1).name));
    return builder.addStatement("return new $T(this, $N)", next, parameter).build();
  }

  private CodeBlock fullInvoke() {
    List<CodeBlock> blocks = basicInvoke();
    CodeBlock invoke = description.unshuffle(blocks)
        .stream()
        .collect(joinCodeBlocks(", "));
    return regularDetailsCases(
        constructor -> statement("return new $T($L)",
            rawClassName(description.context.type), invoke),
        staticMethod -> CodeBlock.builder()
            .add(staticMethod.goalType == VOID ? emptyCodeBlock : CodeBlock.of("return "))
            .addStatement("$T.$L($L)",
                rawClassName(description.context.type),
                staticMethod.methodName, invoke).build(),
        instanceMethod -> CodeBlock.builder()
            .add(instanceMethod.goalType == VOID ? emptyCodeBlock : CodeBlock.of("return "))
            .addStatement("$L.$L($L)",
                instance(),
                instanceMethod.methodName, invoke).build())
        .apply(description.details);
  }

  private CodeBlock instance() {
    CodeBlock.Builder builder = CodeBlock.builder();
    for (int i = description.parameters.size() - 2; i >= 0; i--) {
      builder.add("$L.", description.parameters.get(i).name + "Acc");
    }
    return builder.add("instance").build();
  }

  private IntFunction<CodeBlock> invokeFn() {
    return i -> {
      CodeBlock.Builder block = CodeBlock.builder();
      for (int j = description.parameters.size() - 3; j >= i; j--) {
        String name = description.parameters.get(j + 1).name;
        block.add("$L.", name + "Acc");
      }
      String name = description.parameters.get(i).name;
      block.add("$L", name);
      return block.build();
    };
  }

  List<CodeBlock> basicInvoke() {
    return IntStream.range(0, description.parameters.size())
        .mapToObj(invokeFn())
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

  GenericsImpl(ClassName contract, SimpleRegularGoalDescription description) {
    this.contract = contract;
    this.description = description;
  }
}
