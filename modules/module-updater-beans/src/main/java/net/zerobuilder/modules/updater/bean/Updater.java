package net.zerobuilder.modules.updater.bean;

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
import net.zerobuilder.compiler.generate.DtoStep.CollectionInfo;
import net.zerobuilder.compiler.generate.ZeroUtil;

import java.util.ArrayList;
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
import static net.zerobuilder.compiler.generate.ZeroUtil.ClassNames.ITERABLE;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.flatList;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.presentInstances;
import static net.zerobuilder.modules.updater.bean.BeanUpdater.implType;

final class Updater {

  final static Function<BeanGoalContext, List<FieldSpec>> fields =
      goal -> goal.mayReuse() ?
          asList(
              fieldSpec(BOOLEAN, "_currently_in_use", PRIVATE),
              goal.bean()) :
          singletonList(goal.bean());

  final static Function<BeanGoalContext, List<MethodSpec>> stepMethods = goal ->
      goal.steps.stream()
          .map(stepToMethods(goal))
          .collect(flatList());

  private static Function<AbstractBeanStep, List<MethodSpec>> stepToMethods(BeanGoalContext goal) {
    return beanStepCases(
        step -> regularMethods(step, goal),
        step -> collectionUpdaters(goal, step));
  }

  private static List<MethodSpec> regularMethods(AccessorPairStep step, BeanGoalContext goal) {
    List<MethodSpec> builder = new ArrayList<>();
    builder.add(normalUpdate(goal, step));
    builder.addAll(presentInstances(regularEmptyCollection(goal, step)));
    return builder;
  }

  private static Optional<MethodSpec> regularEmptyCollection(BeanGoalContext goal, AccessorPairStep step) {
    Optional<CollectionInfo> maybeEmptyOption = step.emptyOption();
    if (!maybeEmptyOption.isPresent()) {
      return Optional.empty();
    }
    CollectionInfo collectionInfo = maybeEmptyOption.get();
    TypeName type = step.accessorPair.type;
    String name = step.accessorPair.name();
    ParameterSpec emptyColl = parameterSpec(type, name);
    return Optional.of(methodBuilder(collectionInfo.name)
        .returns(implType(goal))
        .addExceptions(step.accessorPair.setterThrownTypes)
        .addStatement("$T $N = $L", emptyColl.type, emptyColl, collectionInfo.initializer)
        .addStatement("this.$N.$L($N)",
            goal.bean(), step.accessorPair.setterName(), emptyColl)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build());
  }

  private static MethodSpec normalUpdate(BeanGoalContext goal, AccessorPairStep step) {
    String name = step.accessorPair.name();
    ParameterSpec parameter = step.parameter();
    return methodBuilder(name)
        .returns(implType(goal))
        .addExceptions(step.accessorPair.setterThrownTypes)
        .addParameter(parameter)
        .addStatement("this.$N.$L($N)",
            goal.bean(), step.accessorPair.setterName(), parameter)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static List<MethodSpec> collectionUpdaters(BeanGoalContext goal, LoneGetterStep step) {
    List<MethodSpec> builder = new ArrayList<>();
    builder.add(iterateCollection(goal, step));
    builder.add(loneGetterEmptyCollection(goal, step));
    return builder;
  }

  private static MethodSpec iterateCollection(BeanGoalContext goal, LoneGetterStep step) {
    ParameterizedTypeName iterable = ParameterizedTypeName.get(ITERABLE,
        subtypeOf(step.loneGetter.iterationType()));
    String name = step.loneGetter.name();
    ParameterSpec parameter = parameterSpec(iterable, name);
    ParameterSpec iterationVar = step.loneGetter.iterationVar(parameter);
    return methodBuilder(name)
        .returns(implType(goal))
        .addParameter(parameter)
        .addExceptions(step.loneGetter.getterThrownTypes)
        .addCode(ZeroUtil.nullCheck(name, name))
        .addCode(clearCollection(goal, step))
        .beginControlFlow("for ($T $N : $N)", iterationVar.type, iterationVar, name)
        .addStatement("this.$N.$N().add($N)",
            goal.bean(), step.loneGetter.getter, iterationVar)
        .endControlFlow()
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec loneGetterEmptyCollection(BeanGoalContext goal, LoneGetterStep step) {
    return methodBuilder(step.emptyMethod)
        .returns(implType(goal))
        .addExceptions(step.loneGetter.getterThrownTypes)
        .addCode(clearCollection(goal, step))
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static CodeBlock clearCollection(BeanGoalContext goal, LoneGetterStep step) {
    return CodeBlock.builder().addStatement("this.$N.$N().clear()",
        goal.bean(), step.loneGetter.getter).build();
  }

  private Updater() {
    throw new UnsupportedOperationException("no instances");
  }
}