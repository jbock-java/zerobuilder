package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.LoneGetterStep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoBeanStep.beanStepCases;
import static net.zerobuilder.compiler.generate.DtoStep.AbstractStep.nextType;
import static net.zerobuilder.compiler.generate.Step.nullCheck;
import static net.zerobuilder.compiler.generate.ZeroUtil.ClassNames.ITERABLE;
import static net.zerobuilder.compiler.generate.ZeroUtil.flatList;
import static net.zerobuilder.compiler.generate.ZeroUtil.nullCheck;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.presentInstances;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;

final class BuilderB {

  private final Builder builder;

  BuilderB(Builder builder) {
    this.builder = builder;
  }

  final Function<BeanGoalContext, List<FieldSpec>> fieldsB
      = goal -> singletonList(goal.bean());

  final Function<BeanGoalContext, List<MethodSpec>> stepsB =
      goal -> goal.steps.stream()
          .map(stepToMethods(goal))
          .collect(flatList());

  private Function<AbstractBeanStep, List<MethodSpec>> stepToMethods(
      BeanGoalContext goal) {
    return beanStepCases(
        step -> regularMethods(step, goal),
        step -> collectionMethods(step, goal));
  }

  private List<MethodSpec> regularMethods(AccessorPairStep step, BeanGoalContext goal) {
    List<MethodSpec> builder = new ArrayList<>();
    builder.add(regularStep(step, goal));
    builder.addAll(presentInstances(regularEmptyCollection(step, goal)));
    return builder;
  }

  private Optional<MethodSpec> regularEmptyCollection(AccessorPairStep step, BeanGoalContext goal) {
    Optional<DtoStep.CollectionInfo> maybeEmptyOption = step.emptyOption();
    if (!maybeEmptyOption.isPresent()) {
      return Optional.empty();
    }
    DtoStep.CollectionInfo collectionInfo = maybeEmptyOption.get();
    TypeName type = step.accessorPair.type;
    String name = step.accessorPair.name();
    ParameterSpec emptyColl = parameterSpec(type, name);
    return Optional.of(methodBuilder(collectionInfo.name)
        .addAnnotation(Override.class)
        .addExceptions(step.accessorPair.setterThrownTypes)
        .returns(nextType(step))
        .addStatement("$T $N = $L", emptyColl.type, emptyColl, collectionInfo.initializer)
        .addStatement("this.$N.$L($N)", goal.bean(), step.accessorPair.setterName(), emptyColl)
        .addCode(regularFinalBlock(goal, step.isLast()))
        .addModifiers(PUBLIC)
        .build());
  }

  private List<MethodSpec> collectionMethods(LoneGetterStep step, BeanGoalContext goal) {
    return Arrays.asList(
        iterateCollection(step, goal),
        loneGetterEmptyCollection(step, goal));
  }

  private MethodSpec loneGetterEmptyCollection(LoneGetterStep step, BeanGoalContext goal) {
    return methodBuilder(step.emptyMethod)
        .addAnnotation(Override.class)
        .returns(nextType(step))
        .addCode(regularFinalBlock(goal, step.isLast()))
        .addModifiers(PUBLIC)
        .build();
  }

  private CodeBlock regularFinalBlock(BeanGoalContext goal, boolean isLast) {
    return isLast
        ? statement("return this.$N", goal.bean())
        : statement("return this");
  }

  private MethodSpec iterateCollection(LoneGetterStep step, BeanGoalContext goal) {
    String name = step.loneGetter.name();
    ParameterizedTypeName iterable = ParameterizedTypeName.get(ITERABLE,
        subtypeOf(step.loneGetter.iterationType()));
    ParameterSpec parameter = parameterSpec(iterable, name);
    ParameterSpec iterationVar = step.loneGetter.iterationVar(parameter);
    return methodBuilder(name)
        .addAnnotation(Override.class)
        .returns(nextType(step))
        .addExceptions(step.loneGetter.getterThrownTypes)
        .addParameter(parameter)
        .addCode(nullCheck(parameter))
        .beginControlFlow("for ($T $N : $N)",
            iterationVar.type, iterationVar, parameter)
        .addStatement("this.$N.$L().add($N)", goal.bean(),
            step.loneGetter.getter, iterationVar)
        .endControlFlow()
        .addCode(regularFinalBlock(goal, step.isLast()))
        .addModifiers(PUBLIC)
        .build();
  }

  private MethodSpec regularStep(AccessorPairStep step, BeanGoalContext goal) {
    ParameterSpec parameter = step.parameter();
    return methodBuilder(step.accessorPair.name())
        .addAnnotation(Override.class)
        .addExceptions(step.accessorPair.setterThrownTypes)
        .addParameter(parameter)
        .addModifiers(PUBLIC)
        .returns(nextType(step))
        .addCode(nullCheck.apply(step))
        .addStatement("this.$N.$L($N)", goal.bean(), step.accessorPair.setterName(), parameter)
        .addCode(regularFinalBlock(goal, step.isLast())).build();
  }
}
