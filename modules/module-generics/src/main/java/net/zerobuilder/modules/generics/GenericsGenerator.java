package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;

import java.util.List;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.concat;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.nullCheck;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.modules.generics.GenericsContract.contractType;
import static net.zerobuilder.modules.generics.GenericsContract.implType;
import static net.zerobuilder.modules.generics.GenericsContract.stepInterfaces;
import static net.zerobuilder.modules.generics.GenericsContract.stepTypes;
import static net.zerobuilder.modules.generics.VarLife.dependents;
import static net.zerobuilder.modules.generics.VarLife.implTypeParams;
import static net.zerobuilder.modules.generics.VarLife.methodParams;
import static net.zerobuilder.modules.generics.VarLife.typeParams;
import static net.zerobuilder.modules.generics.VarLife.varLifes;

final class GenericsGenerator {

  private final List<TypeSpec> stepSpecs;
  private final List<List<TypeVariableName>> methodParams;
  private final List<List<TypeVariableName>> implTypeParams;
  private final ClassName implType;
  private final ClassName contractType;
  private final GenericsImpl impl;
  private final List<TypeSpec> stepImpls;
  private final SimpleStaticMethodGoalContext goal;

  private GenericsGenerator(List<TypeSpec> stepSpecs,
                            List<List<TypeVariableName>> methodParams,
                            List<List<TypeVariableName>> implTypeParams,
                            SimpleStaticMethodGoalContext goal) {
    this.stepSpecs = stepSpecs;
    this.methodParams = methodParams;
    this.implTypeParams = implTypeParams;
    this.implType = implType(goal);
    this.contractType = contractType(goal);
    this.impl = new GenericsImpl(implType, contractType, goal);
    this.goal = goal;
    this.stepImpls = impl.stepImpls(stepSpecs, methodParams, implTypeParams);
  }

  TypeSpec defineImpl() {
    ClassName firstImplType = implType.nestedClass(stepImpls.get(0).name);
    TypeSpec.Builder builder = classBuilder(implType)
        .addModifiers(PRIVATE, STATIC, FINAL);
    if (!goal.details.instance) {
      builder.addField(FieldSpec.builder(firstImplType,
          downcase(firstImplType.simpleName()), PRIVATE, STATIC, FINAL)
          .initializer("new $T()", firstImplType).build());
    }
    return builder
        .addTypes(stepImpls)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE)
            .build())
        .build();
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

  DtoGeneratorOutput.BuilderMethod builderMethod(SimpleStaticMethodGoalContext goal) {
    ParameterSpec instance = parameterSpec(goal.context.type, "instance");
    MethodSpec.Builder builder = methodBuilder(goal.details.name + "Builder")
        .addModifiers(goal.details.access(STATIC))
        .returns(contractType.nestedClass(stepSpecs.get(0).name));
    if (goal.details.instance) {
      builder.addParameter(instance);
      builder.addCode(nullCheck(instance));
    }
    builder.addTypeVariables(goal.details.instanceTypeParameters);
    builder.addCode(goal.details.instance ?
        statement("return new $T($N)", implType.nestedClass(stepImpls.get(0).name), instance) :
        statement("return $T.$L", implType, downcase(stepImpls.get(0).name)));
    return new DtoGeneratorOutput.BuilderMethod(
        goal.details.name,
        builder.build());
  }

  static GenericsGenerator create(SimpleStaticMethodGoalContext goal) {
    List<List<TypeVariableName>> lifes = varLifes(
        concat(goal.details.instanceTypeParameters, goal.details.typeParameters),
        stepTypes(goal),
        goal.details.instanceTypeParameters);
    List<TypeVariableName> dependents = dependents(goal.details.instanceTypeParameters, goal.details.typeParameters);
    List<List<TypeVariableName>> typeParams = typeParams(lifes, dependents);
    List<List<TypeVariableName>> implTypeParams = implTypeParams(lifes, dependents);
    List<List<TypeVariableName>> methodParams = methodParams(lifes, dependents);
    List<TypeSpec> stepSpecs = stepInterfaces(goal, typeParams, methodParams);
    return new GenericsGenerator(stepSpecs, methodParams, implTypeParams, goal);
  }
}
