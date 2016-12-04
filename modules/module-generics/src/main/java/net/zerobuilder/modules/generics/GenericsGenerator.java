package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoGoalDetails.regularDetailsCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.concat;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.nullCheck;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.modules.generics.GenericsContract.contractType;
import static net.zerobuilder.modules.generics.GenericsContract.implType;
import static net.zerobuilder.modules.generics.GenericsContract.stepInterfaces;
import static net.zerobuilder.modules.generics.GenericsContract.stepTypes;
import static net.zerobuilder.modules.generics.VarLife.implTypeParams;
import static net.zerobuilder.modules.generics.VarLife.methodParams;
import static net.zerobuilder.modules.generics.VarLife.typeParams;
import static net.zerobuilder.modules.generics.VarLife.varLifes;

final class GenericsGenerator {

  final List<TypeSpec> stepSpecs;
  final List<List<TypeVariableName>> methodParams;
  final List<List<TypeVariableName>> implTypeParams;
  final ClassName implType;
  final ClassName contractType;
  final GenericsImpl impl;
  final List<TypeSpec> stepImpls;
  final SimpleRegularGoalDescription description;

  private GenericsGenerator(List<TypeSpec> stepSpecs,
                            List<List<TypeVariableName>> methodParams,
                            List<List<TypeVariableName>> implTypeParams,
                            SimpleRegularGoalDescription description) {
    this.stepSpecs = stepSpecs;
    this.methodParams = methodParams;
    this.implTypeParams = implTypeParams;
    this.implType = implType(description);
    this.contractType = contractType(description);
    this.impl = new GenericsImpl(implType, contractType, description);
    this.description = description;
    this.stepImpls = impl.stepImpls(stepSpecs, methodParams, implTypeParams);
  }

  TypeSpec defineImpl() {
    TypeSpec.Builder builder = classBuilder(implType)
        .addModifiers(PRIVATE, STATIC, FINAL);
    builder.addFields(firstStepCache.apply(description.details));
    return builder
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
    ClassName firstImplType = implType.nestedClass(stepImpls.get(0).name);
    return singletonList(FieldSpec.builder(firstImplType,
        downcase(firstImplType.simpleName()), PRIVATE, STATIC, FINAL)
        .initializer("new $T()", firstImplType).build());
  }

  TypeSpec defineContract() {
    return classBuilder(contractType)
        .addTypes(stepSpecs)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE)
            .build())
        .build();
  }

  DtoGeneratorOutput.BuilderMethod builderMethod(SimpleRegularGoalDescription description) {
    ParameterSpec instance = parameterSpec(description.context.type, "instance");
    MethodSpec.Builder builder = methodBuilder(description.details.name + "Builder")
        .addModifiers(description.details.access(STATIC))
        .returns(parameterizedTypeName(
            contractType.nestedClass(stepSpecs.get(0).name),
            stepSpecs.get(0).typeVariables));
    builder.addCode(regularDetailsCases(
        constructor -> emptyCodeBlock,
        staticMethod -> emptyCodeBlock,
        instanceMethod -> nullCheck(instance)
    ).apply(description.details));
    builder.addParameters(
        regularDetailsCases(
            constructor -> Collections.<ParameterSpec>emptyList(),
            staticMethod -> Collections.<ParameterSpec>emptyList(),
            instanceMethod -> singletonList(instance)
        ).apply(description.details));
    builder.addTypeVariables(new HashSet<>(
        concat(instanceMethodTypeParameters.apply(description.details),
            stepSpecs.get(0).typeVariables)));
    builder.addCode(regularDetailsCases(
        constructor -> statement("return $T.$L", implType, downcase(stepImpls.get(0).name)),
        staticMethod -> statement("return $T.$L", implType, downcase(stepImpls.get(0).name)),
        instanceMethod -> statement("return new $T($N)", implType.nestedClass(stepImpls.get(0).name), instance)
    ).apply(description.details));
    return new DtoGeneratorOutput.BuilderMethod(
        description.details.name,
        builder.build());
  }

  private final Function<AbstractRegularDetails, List<TypeVariableName>> instanceMethodTypeParameters =
      regularDetailsCases(
          constructor -> Collections.<TypeVariableName>emptyList(),
          staticMethod -> Collections.<TypeVariableName>emptyList(),
          instanceMethod -> instanceMethod.instanceTypeParameters);

  static GenericsGenerator create(SimpleRegularGoalDescription description) {
    AbstractRegularDetails details = description.details;
    List<TypeVariableName> dependents = GenericsGenerator.dependents(description)
        .apply(description.details);
    List<List<TypeVariableName>> lifes = varLifes(
        GenericsGenerator.allTypeParameters.apply(details),
        stepTypes(description),
        dependents);
    List<List<TypeVariableName>> typeParams = typeParams(lifes, dependents);
    List<List<TypeVariableName>> implTypeParams = implTypeParams(lifes, dependents);
    List<List<TypeVariableName>> methodParams = methodParams(lifes, dependents);
    List<TypeSpec> stepSpecs = stepInterfaces(description, typeParams, methodParams);
    return new GenericsGenerator(stepSpecs, methodParams, implTypeParams, description);
  }

  private static final Function<AbstractRegularDetails, List<TypeVariableName>> allTypeParameters =
      regularDetailsCases(
          constructor -> constructor.instanceTypeParameters,
          staticMethod -> staticMethod.typeParameters,
          instanceMethod -> concat(instanceMethod.instanceTypeParameters, instanceMethod.typeParameters));

  private static Function<AbstractRegularDetails, List<TypeVariableName>> dependents(SimpleRegularGoalDescription description) {
    return regularDetailsCases(
        constructor -> Collections.<TypeVariableName>emptyList(),
        staticMethod -> Collections.<TypeVariableName>emptyList(),
        instanceMethod -> VarLife.dependents(instanceMethod.instanceTypeParameters,
            stepTypes(description)));
  }
}
