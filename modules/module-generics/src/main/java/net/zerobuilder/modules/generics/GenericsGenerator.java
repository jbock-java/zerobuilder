package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;

import java.util.List;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.modules.generics.GenericsContract.contractType;
import static net.zerobuilder.modules.generics.GenericsContract.implType;
import static net.zerobuilder.modules.generics.GenericsContract.stepInterfaces;
import static net.zerobuilder.modules.generics.GenericsContract.stepTypes;
import static net.zerobuilder.modules.generics.VarLife.implTypeParams;
import static net.zerobuilder.modules.generics.VarLife.methodParams;
import static net.zerobuilder.modules.generics.VarLife.typeParams;
import static net.zerobuilder.modules.generics.VarLife.varLifes;

final class GenericsGenerator {

  private final List<TypeSpec> stepSpecs;
  private final List<List<TypeVariableName>> methodParams;
  private final List<List<TypeVariableName>> typeParams;
  private final List<List<TypeVariableName>> implTypeParams;
  private final SimpleStaticMethodGoalContext goal;
  private final ClassName implType;
  private final GenericsImpl impl;

  private GenericsGenerator(List<TypeSpec> stepSpecs,
                            List<List<TypeVariableName>> methodParams,
                            List<List<TypeVariableName>> typeParams,
                            List<List<TypeVariableName>> implTypeParams,
                            SimpleStaticMethodGoalContext goal) {
    this.stepSpecs = stepSpecs;
    this.methodParams = methodParams;
    this.typeParams = typeParams;
    this.implTypeParams = implTypeParams;
    this.goal = goal;
    this.implType = implType(goal);
    this.impl = new GenericsImpl(implType);
  }

  TypeSpec defineImpl() {
    return classBuilder(implType)
        .addModifiers(STATIC, FINAL)
        .addTypes(impl.stepImpls(implType, stepSpecs, methodParams, implTypeParams))
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE)
            .build())
        .build();
  }

  TypeSpec defineContract() {
    return classBuilder(contractType(goal))
        .addTypes(stepSpecs)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE)
            .build())
        .build();
  }

  static GenericsGenerator create(SimpleStaticMethodGoalContext goal) {
    List<List<TypeVariableName>> lifes = varLifes(goal.details.typeParameters, stepTypes(goal));
    List<List<TypeVariableName>> typeParams = typeParams(lifes);
    List<List<TypeVariableName>> implTypeParams = implTypeParams(lifes);
    List<List<TypeVariableName>> methodParams = methodParams(lifes);
    List<TypeSpec> stepSpecs = stepInterfaces(goal, typeParams, methodParams);
    return new GenericsGenerator(stepSpecs, methodParams, typeParams, implTypeParams, goal);
  }
}
