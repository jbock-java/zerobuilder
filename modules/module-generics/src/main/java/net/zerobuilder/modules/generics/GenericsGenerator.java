package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import java.util.HashSet;
import java.util.List;
import java.util.function.BiFunction;
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
import static net.zerobuilder.compiler.generate.DtoGoalDetails.isInstance;
import static net.zerobuilder.compiler.generate.DtoGoalDetails.regularDetailsCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.concat;
import static net.zerobuilder.compiler.generate.ZeroUtil.cons;
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

final class GenericsGenerator {

  private final List<TypeSpec> stepSpecs;
  final List<List<TypeVariableName>> methodParams;
  final List<List<TypeVariableName>> implTypeParams;
  private final ClassName implType;
  private final ClassName contractType;
  private final List<TypeSpec> stepImpls;
  private final SimpleRegularGoalDescription description;

  private GenericsGenerator(List<TypeSpec> stepSpecs,
                            List<List<TypeVariableName>> methodParams,
                            List<List<TypeVariableName>> implTypeParams,
                            SimpleRegularGoalDescription description,
                            ClassName implType,
                            ClassName contractType,
                            List<TypeSpec> stepImpls) {
    this.stepSpecs = stepSpecs;
    this.methodParams = methodParams;
    this.implTypeParams = implTypeParams;
    this.implType = implType;
    this.contractType = contractType;
    this.description = description;
    this.stepImpls = stepImpls;
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
    builder.addCode(goalMethodNullCheck
        .apply(description.details, instance));
    builder.addParameters(
        goalMethodParameters.apply(description.details, instance));
    builder.addTypeVariables(new HashSet<>(
        instanceMethodTypeParameters.apply(description.details,
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

  private final BiFunction<AbstractRegularDetails, ParameterSpec, CodeBlock> goalMethodNullCheck =
      regularDetailsCases(
          (constructor, instance) -> emptyCodeBlock,
          (staticMethod, instance) -> emptyCodeBlock,
          (instanceMethod, instance) -> nullCheck(instance));

  private final BiFunction<AbstractRegularDetails, ParameterSpec, List<ParameterSpec>> goalMethodParameters =
      regularDetailsCases(
          (constructor, instance) -> emptyList(),
          (staticMethod, instance) -> emptyList(),
          (instanceMethod, instance) -> singletonList(instance));

  private final BiFunction<AbstractRegularDetails, List<TypeVariableName>, List<TypeVariableName>> instanceMethodTypeParameters =
      regularDetailsCases(
          (constructor, firstStepTypeParameters) -> firstStepTypeParameters,
          (staticMethod, firstStepTypeParameters) -> firstStepTypeParameters,
          (instanceMethod, firstStepTypeParameters) -> concat(
              firstStepTypeParameters,
              instanceMethod.instanceTypeParameters));

  static GenericsGenerator create(SimpleRegularGoalDescription description) {
    AbstractRegularDetails details = description.details;
    List<TypeVariableName> typeParameters = GenericsGenerator.allTypeParameters.apply(details);
    VarLife lifes = VarLife.create(
        typeParameters,
        GenericsGenerator.extendedStepTypes.apply(description.details, description),
        isInstance.apply(details));
    List<List<TypeVariableName>> typeParams = lifes.typeParams();
    List<List<TypeVariableName>> implTypeParams = lifes.implTypeParams();
    List<List<TypeVariableName>> methodParams = lifes.methodParams();
    List<TypeSpec> stepSpecs = stepInterfaces(description, typeParams, methodParams);
    ClassName implType = implType(description);
    ClassName contractType = contractType(description);
    GenericsImpl genericsImpl = new GenericsImpl(implType, contractType, description);
    List<TypeSpec> stepImpls = genericsImpl.stepImpls(stepSpecs, methodParams, implTypeParams);
    return new GenericsGenerator(stepSpecs, methodParams, implTypeParams, description,
        implType, contractType, stepImpls);
  }

  private static final Function<AbstractRegularDetails, List<TypeVariableName>> allTypeParameters =
      regularDetailsCases(
          constructor -> constructor.instanceTypeParameters,
          staticMethod -> staticMethod.typeParameters,
          instanceMethod -> concat(
              instanceMethod.instanceTypeParameters, instanceMethod.typeParameters));

  private static final BiFunction<AbstractRegularDetails, SimpleRegularGoalDescription, List<TypeName>> extendedStepTypes =
      regularDetailsCases(
          (constructor, description) -> stepTypes(description),
          (staticMethod, description) -> stepTypes(description),
          (instanceMethod, description) -> cons(
              description.context.type,
              stepTypes(description)));

}
