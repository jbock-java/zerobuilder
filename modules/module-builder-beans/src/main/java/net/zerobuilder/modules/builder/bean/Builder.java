package net.zerobuilder.modules.builder.bean;

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
import net.zerobuilder.compiler.generate.DtoStep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeName.BOOLEAN;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoBeanStep.beanStepCases;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.ZeroUtil.ClassNames.ITERABLE;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.flatList;
import static net.zerobuilder.compiler.generate.ZeroUtil.nullCheck;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.presentInstances;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.modules.builder.bean.BeanStep.nextType;

final class Builder {

  static final Function<BeanGoalContext, List<FieldSpec>> fieldsB =
      goal -> goal.context().lifecycle == REUSE_INSTANCES ?
          asList(
              fieldSpec(BOOLEAN, "_currently_in_use", PRIVATE),
              goal.bean()) :
          singletonList(goal.bean());

  static final Function<BeanGoalContext, List<MethodSpec>> stepsB =
      goal -> goal.steps.stream()
          .map(stepToMethods(goal))
          .collect(flatList());

  static Function<AbstractBeanStep, List<MethodSpec>> stepToMethods(
      BeanGoalContext goal) {
    return beanStepCases(
        step -> regularMethods(step, goal),
        step -> collectionMethods(step, goal));
  }

  static List<MethodSpec> regularMethods(AccessorPairStep step, BeanGoalContext goal) {
    List<MethodSpec> builder = new ArrayList<>();
    builder.add(regularStep(step, goal));
    builder.addAll(presentInstances(regularEmptyCollection(step, goal)));
    return builder;
  }

  static Optional<MethodSpec> regularEmptyCollection(AccessorPairStep step, BeanGoalContext goal) {
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
        .addCode(step.isLast() ? normalReturn(goal) : statement("return this"))
        .addModifiers(PUBLIC)
        .build());
  }

  static List<MethodSpec> collectionMethods(LoneGetterStep step, BeanGoalContext goal) {
    return Arrays.asList(
        iterateCollection(step, goal),
        loneGetterEmptyCollection(step, goal));
  }

  static MethodSpec loneGetterEmptyCollection(LoneGetterStep step, BeanGoalContext goal) {
    return methodBuilder(step.emptyMethod)
        .addAnnotation(Override.class)
        .returns(nextType(step))
        .addCode(step.isLast() ? normalReturn(goal) : statement("return this"))
        .addModifiers(PUBLIC)
        .build();
  }

  static CodeBlock normalReturn(BeanGoalContext goal) {
    ParameterSpec varBean = parameterSpec(goal.type(),
        '_' + downcase(goal.details.goalType.simpleName()));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (goal.context.lifecycle == REUSE_INSTANCES) {
      builder.addStatement("this._currently_in_use = false");
    }
    builder.addStatement("$T $N = this.$N", varBean.type, varBean, goal.bean());
    if (goal.context.lifecycle == REUSE_INSTANCES) {
      builder.addStatement("this.$N = null", goal.bean());
    }
    return builder.addStatement("return $N", varBean).build();
  }

  static MethodSpec iterateCollection(LoneGetterStep step, BeanGoalContext goal) {
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
        .addCode(step.isLast() ? normalReturn(goal) : statement("return this"))
        .addModifiers(PUBLIC)
        .build();
  }

  static MethodSpec regularStep(AccessorPairStep step, BeanGoalContext goal) {
    ParameterSpec parameter = step.parameter();
    return methodBuilder(step.accessorPair.name())
        .addAnnotation(Override.class)
        .addExceptions(step.accessorPair.setterThrownTypes)
        .addParameter(parameter)
        .addModifiers(PUBLIC)
        .returns(nextType(step))
        .addCode(Step.nullCheck.apply(step))
        .addStatement("this.$N.$L($N)", goal.bean(), step.accessorPair.setterName(), parameter)
        .addCode(step.isLast() ? normalReturn(goal) : statement("return this"))
        .build();
  }

  private Builder() {
    throw new UnsupportedOperationException("no instances");
  }
}
