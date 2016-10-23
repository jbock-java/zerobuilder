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
import net.zerobuilder.compiler.generate.DtoStep.CollectionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoBeanStep.beanStepCases;
import static net.zerobuilder.compiler.generate.Utilities.ClassNames.ITERABLE;
import static net.zerobuilder.compiler.generate.Utilities.flatList;
import static net.zerobuilder.compiler.generate.Utilities.nullCheck;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.presentInstances;

final class UpdaterB {

  private final Updater updater;

  UpdaterB(Updater updater) {
    this.updater = updater;
  }

  final Function<BeanGoalContext, List<FieldSpec>> fieldsB
      = goal -> singletonList(goal.bean());

  final Function<BeanGoalContext, List<MethodSpec>> updateMethodsB = goal ->
      goal.steps.stream()
          .map(stepToMethods(goal))
          .collect(flatList());

  private Function<AbstractBeanStep, List<MethodSpec>> stepToMethods(BeanGoalContext goal) {
    return beanStepCases(
        step -> regularMethods(step, goal),
        step -> collectionUpdaters(goal, step));
  }

  private List<MethodSpec> regularMethods(AccessorPairStep step, BeanGoalContext goal) {
    List<MethodSpec> builder = new ArrayList<>();
    builder.add(normalUpdate(goal, step));
    builder.addAll(presentInstances(regularEmptyCollection(goal, step)));
    return builder;
  }

  private Optional<MethodSpec> regularEmptyCollection(BeanGoalContext goal, AccessorPairStep step) {
    Optional<CollectionInfo> maybeEmptyOption = step.emptyOption();
    if (!maybeEmptyOption.isPresent()) {
      return Optional.empty();
    }
    CollectionInfo collectionInfo = maybeEmptyOption.get();
    TypeName type = step.accessorPair.type;
    String name = step.accessorPair.name();
    ParameterSpec emptyColl = parameterSpec(type, name);
    return Optional.of(methodBuilder(collectionInfo.name)
        .returns(goal.implType())
        .addExceptions(step.accessorPair.setterThrownTypes)
        .addStatement("$T $N = $L", emptyColl.type, emptyColl, collectionInfo.initializer)
        .addStatement("this.$N.$L($N)",
            goal.bean(), step.accessorPair.setterName(), emptyColl)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build());
  }

  private MethodSpec normalUpdate(BeanGoalContext goal, AccessorPairStep step) {
    String name = step.accessorPair.name();
    ParameterSpec parameter = step.parameter();
    return methodBuilder(name)
        .returns(goal.implType())
        .addExceptions(step.accessorPair.setterThrownTypes)
        .addParameter(parameter)
        .addStatement("this.$N.$L($N)",
            goal.bean(), step.accessorPair.setterName(), parameter)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private List<MethodSpec> collectionUpdaters(BeanGoalContext goal, LoneGetterStep step) {
    List<MethodSpec> builder = new ArrayList<>();
    builder.add(iterateCollection(goal, step));
    builder.add(loneGetterEmptyCollection(goal, step));
    return builder;
  }

  private MethodSpec iterateCollection(BeanGoalContext goal, LoneGetterStep step) {
    ParameterizedTypeName iterable = ParameterizedTypeName.get(ITERABLE,
        subtypeOf(step.loneGetter.iterationType()));
    String name = step.loneGetter.name();
    ParameterSpec parameter = parameterSpec(iterable, name);
    ParameterSpec iterationVar = step.loneGetter.iterationVar(parameter);
    return methodBuilder(name)
        .returns(goal.implType())
        .addParameter(parameter)
        .addExceptions(step.loneGetter.getterThrownTypes)
        .addCode(nullCheck(name, name))
        .addCode(clearCollection(goal, step))
        .beginControlFlow("for ($T $N : $N)", iterationVar.type, iterationVar, name)
        .addStatement("this.$N.$N().add($N)",
            goal.bean(), step.loneGetter.getter, iterationVar)
        .endControlFlow()
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private MethodSpec loneGetterEmptyCollection(BeanGoalContext goal, LoneGetterStep step) {
    return methodBuilder(step.emptyMethod)
        .returns(goal.implType())
        .addExceptions(step.loneGetter.getterThrownTypes)
        .addCode(clearCollection(goal, step))
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private CodeBlock clearCollection(BeanGoalContext goal, LoneGetterStep step) {
    return CodeBlock.builder().addStatement("this.$N.$N().clear()",
        goal.bean(), step.loneGetter.getter).build();
  }
}
