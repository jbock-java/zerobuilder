package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.LoneGetterStep;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;

import javax.lang.model.element.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static java.util.Arrays.asList;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.NullPolicy.ALLOW;
import static net.zerobuilder.compiler.generate.DtoBeanStep.beanStepCases;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.Utilities.flatList;
import static net.zerobuilder.compiler.generate.Utilities.joinCodeBlocks;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.statement;

final class GeneratorBU {

  static final Function<BeanGoalContext, BuilderMethod> goalToUpdaterB
      = goal -> {
    String name = goal.goal.details.name;
    ClassName type = goal.goal.details.goalType;
    ParameterSpec updater = updaterInstance(goal);
    Modifier[] modifiers = goal.goal.details.option.access
        .modifiers(STATIC);
    MethodSpec method = methodBuilder(downcase(name + "Updater"))
        .addParameter(parameterSpec(type, downcase(type.simpleName())))
        .returns(goal.implType())
        .addExceptions(thrownTypes(goal, asList(AbstractBeanStep::getterThrownTypes, AbstractBeanStep::setterThrownTypes)))
        .addCode(goal.goal.steps.stream().map(nullChecks(goal)).collect(joinCodeBlocks))
        .addCode(initializeUpdater(goal, updater))
        .addCode(goal.goal.steps.stream().map(copy(goal)).collect(joinCodeBlocks))
        .addStatement("return $N", updater)
        .addModifiers(modifiers)
        .build();
    return new BuilderMethod(name, method);
  };

  private static Set<TypeName> thrownTypes(BeanGoalContext goal,
                                           List<Function<AbstractBeanStep, List<TypeName>>> functions) {
    Set<TypeName> thrownTypes = new HashSet<>();
    for (Function<AbstractBeanStep, List<TypeName>> function : functions) {
      thrownTypes.addAll(goal.goal.steps.stream()
          .map(function)
          .collect(flatList()));
    }
    if (goal.context.lifecycle == NEW_INSTANCE) {
      thrownTypes.addAll(goal.goal.thrownTypes);
    }
    return thrownTypes;
  }

  private static Function<AbstractBeanStep, CodeBlock> copy(BeanGoalContext goal) {
    return beanStepCases(
        step -> copyRegular(goal, step),
        step -> copyCollection(goal, step));
  }

  private static Function<AbstractBeanStep, CodeBlock> nullChecks(BeanGoalContext goal) {
    return beanStepCases(
        step -> step.accessorPair.nullPolicy == ALLOW
            ? emptyCodeBlock
            : nullCheck(goal, step.accessorPair),
        step -> nullCheck(goal, step.loneGetter));
  }

  private static CodeBlock copyCollection(BeanGoalContext goal, LoneGetterStep step) {
    ClassName type = goal.goal.details.goalType;
    ParameterSpec parameter = parameterSpec(type, downcase(type.simpleName()));
    ParameterSpec iterationVar = step.loneGetter.iterationVar(parameter);
    return CodeBlock.builder()
        .beginControlFlow("for ($T $N : $N.$N())",
            iterationVar.type, iterationVar, parameter,
            step.loneGetter.getter)
        .addStatement("$N.$N.$N().add($N)", updaterInstance(goal),
            downcase(type.simpleName()),
            step.loneGetter.getter,
            iterationVar)
        .endControlFlow()
        .build();
  }

  private static CodeBlock copyRegular(BeanGoalContext goal, AccessorPairStep step) {
    ClassName type = goal.goal.details.goalType;
    ParameterSpec parameter = parameterSpec(type, downcase(type.simpleName()));
    ParameterSpec updater = updaterInstance(goal);
    return CodeBlock.builder()
        .addStatement("$N.$N.$L($N.$N())", updater,
            goal.bean(),
            step.accessorPair.setterName(),
            parameter,
            step.accessorPair.getter)
        .build();
  }

  private static CodeBlock nullCheck(BeanGoalContext goal, AbstractBeanParameter beanParameter) {
    ClassName type = goal.goal.details.goalType;
    ParameterSpec parameter = parameterSpec(type, downcase(type.simpleName()));
    return CodeBlock.builder()
        .beginControlFlow("if ($N.$N() == null)", parameter,
            beanParameter.getter)
        .addStatement("throw new $T($S)",
            NullPointerException.class, beanParameter.name())
        .endControlFlow().build();
  }

  private static CodeBlock initializeUpdater(BeanGoalContext goal, ParameterSpec updater) {
    CodeBlock.Builder builder = CodeBlock.builder();
    FieldSpec cache = goal.context.cache.get();
    ClassName type = goal.goal.details.goalType;
    builder.add(goal.context.lifecycle == REUSE_INSTANCES
        ? statement("$T $N = $N.get().$N", updater.type, updater, cache, goal.cacheField())
        : statement("$T $N = new $T()", updater.type, updater, updater.type));
    builder.add(goal.context.lifecycle == REUSE_INSTANCES
        ? statement("$N.$N = new $T()", updater, goal.bean(), type)
        : emptyCodeBlock);
    return builder.build();
  }

  private static ParameterSpec updaterInstance(BeanGoalContext goal) {
    ClassName updaterType = goal.implType();
    return parameterSpec(updaterType, "updater");
  }

  private GeneratorBU() {
    throw new UnsupportedOperationException("no instances");
  }
}
