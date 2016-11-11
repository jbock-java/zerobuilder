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
import static net.zerobuilder.modules.generics.GenericsImpl.stepImpls;
import static net.zerobuilder.modules.generics.VarLife.methodParams;
import static net.zerobuilder.modules.generics.VarLife.typeParams;
import static net.zerobuilder.modules.generics.VarLife.varLifes;

final class GenericsGenerator {

  private final List<TypeSpec> stepSpecs;
  private final List<List<TypeVariableName>> methodParams;
  private final SimpleStaticMethodGoalContext goal;

  private GenericsGenerator(List<TypeSpec> stepSpecs,
                            List<List<TypeVariableName>> methodParams,
                            SimpleStaticMethodGoalContext goal) {
    this.stepSpecs = stepSpecs;
    this.methodParams = methodParams;
    this.goal = goal;
  }

  TypeSpec defineImpl() {
    ClassName implType = implType(goal);
    return classBuilder(implType)
        .addModifiers(STATIC, FINAL)
        .addTypes(stepImpls(implType, stepSpecs, methodParams))
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
    List<List<TypeVariableName>> methodParams = methodParams(lifes);
    List<TypeSpec> stepSpecs = stepInterfaces(goal, typeParams, methodParams);
    return new GenericsGenerator(stepSpecs, methodParams, goal);
  }
}
