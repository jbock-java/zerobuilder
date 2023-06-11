package net.zerobuilder.modules.generics;

import io.jbock.javapoet.ClassName;
import io.jbock.javapoet.CodeBlock;
import io.jbock.javapoet.FieldSpec;
import io.jbock.javapoet.MethodSpec;
import io.jbock.javapoet.ParameterSpec;
import io.jbock.javapoet.TypeSpec;
import io.jbock.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.jbock.javapoet.MethodSpec.constructorBuilder;
import static io.jbock.javapoet.MethodSpec.methodBuilder;
import static io.jbock.javapoet.TypeSpec.classBuilder;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoGoalDetails.regularDetailsCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.generics.GenericsContract.implType;

final class GenericsGenerator {

  private final ClassName contractType;
  private final List<TypeSpec> stepImpls;
  private final SimpleRegularGoalDescription description;

  private final BiFunction<AbstractRegularDetails, ParameterSpec, CodeBlock> returnStatement;

  private GenericsGenerator(SimpleRegularGoalDescription description,
                            ClassName contractType,
                            List<TypeSpec> stepImpls) {
    this.contractType = contractType;
    this.description = description;
    this.stepImpls = stepImpls;
    returnStatement = regularDetailsCases(
        (constructor, instance) -> statement("return $T.$L", contractType, downcase(stepImpls.get(0).name)),
        (staticMethod, instance) -> statement("return $T.$L", contractType, downcase(stepImpls.get(0).name)),
        (instanceMethod, instance) -> statement("return new $T($N)", contractType.nestedClass(stepImpls.get(0).name), instance));
  }

  TypeSpec defineImpl() {
    return classBuilder(contractType)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addFields(firstStepCache.apply(description.details))
        .addTypes(stepImpls)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE)
            .build())
        .build();
  }

  private Function<AbstractRegularDetails, List<FieldSpec>> firstStepCache =
      regularDetailsCases(
          constructor -> firstStepCache(),
          staticMethod -> firstStepCache(),
          instanceMethod -> emptyList());


  private List<FieldSpec> firstStepCache() {
    ClassName firstImplType = contractType.nestedClass(stepImpls.get(0).name);
    return singletonList(FieldSpec.builder(firstImplType,
        downcase(firstImplType.simpleName()), PRIVATE, STATIC, FINAL)
        .initializer("new $T()", firstImplType).build());
  }

  DtoGeneratorOutput.BuilderMethod builderMethod(SimpleRegularGoalDescription description,
                                                 VarLife life) {
    ParameterSpec instance = parameterSpec(description.context.type, "instance");
    List<List<TypeVariableName>> typeParams = life.typeParams();
    MethodSpec.Builder builder = methodBuilder(description.details.name + "Builder")
        .addModifiers(description.details.access(STATIC))
        .returns(parameterizedTypeName(
            contractType.nestedClass(upcase(description.parameters.get(0).name)),
            typeParams.get(0)));
    builder.addParameters(
        goalMethodParameters.apply(description.details, instance));
    builder.addTypeVariables(typeParams.get(0));
    builder.addCode(returnStatement.apply(description.details, instance));
    return new DtoGeneratorOutput.BuilderMethod(
        description.details.name,
        builder.build());
  }

  private final BiFunction<AbstractRegularDetails, ParameterSpec, List<ParameterSpec>> goalMethodParameters =
      regularDetailsCases(
          (constructor, instance) -> emptyList(),
          (staticMethod, instance) -> emptyList(),
          (instanceMethod, instance) -> singletonList(instance));

  static GenericsGenerator create(SimpleRegularGoalDescription description, VarLife lifes) {
    List<List<TypeVariableName>> typeParams = lifes.typeParams();
    List<List<TypeVariableName>> methodParams = lifes.methodParams();
    ClassName contractType = implType(description);
    GenericsImpl genericsImpl = new GenericsImpl(contractType, description);
    List<TypeSpec> stepImpls = genericsImpl.stepImpls(methodParams, typeParams);
    return new GenericsGenerator(description, contractType, stepImpls);
  }
}
