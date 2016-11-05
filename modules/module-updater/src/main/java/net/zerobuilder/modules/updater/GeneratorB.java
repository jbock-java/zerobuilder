package net.zerobuilder.modules.updater;

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
import net.zerobuilder.compiler.generate.DtoContext;
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
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.flatList;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.modules.updater.Updater.implType;

final class GeneratorB {

  static BuilderMethod updaterMethodB(BeanGoalContext goal) {
    String name = goal.details.name;
    ClassName type = goal.details.goalType;
    ParameterSpec varUpdater = updaterInstance(goal);
    Modifier[] modifiers = goal.details.access(STATIC);
    MethodSpec method = methodBuilder(downcase(name + "Updater"))
        .addParameter(parameterSpec(type, downcase(type.simpleName())))
        .returns(implType(goal))
        .addExceptions(thrownTypes(goal,
            asList(
                AbstractBeanStep::getterThrownTypes,
                AbstractBeanStep::setterThrownTypes)))
        .addCode(goal.steps.stream().map(nullChecks(goal)).collect(joinCodeBlocks))
        .addCode(initVarUpdater(goal, varUpdater))
        .addCode(goal.steps.stream().map(copy(goal)).collect(joinCodeBlocks))
        .addStatement("return $N", varUpdater)
        .addModifiers(modifiers)
        .build();
    return new BuilderMethod(name, method);
  }

  private static Set<TypeName> thrownTypes(BeanGoalContext goal,
                                           List<Function<AbstractBeanStep, List<TypeName>>> functions) {
    Set<TypeName> thrownTypes = new HashSet<>();
    for (Function<AbstractBeanStep, List<TypeName>> function : functions) {
      thrownTypes.addAll(goal.steps.stream()
          .map(function)
          .collect(flatList()));
    }
    if (goal.context.lifecycle == NEW_INSTANCE) {
      thrownTypes.addAll(goal.thrownTypes);
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
    ClassName type = goal.details.goalType;
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
    ClassName type = goal.details.goalType;
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
    ClassName type = goal.details.goalType;
    ParameterSpec parameter = parameterSpec(type, downcase(type.simpleName()));
    return CodeBlock.builder()
        .beginControlFlow("if ($N.$N() == null)", parameter,
            beanParameter.getter)
        .addStatement("throw new $T($S)",
            NullPointerException.class, beanParameter.name())
        .endControlFlow().build();
  }

  private static CodeBlock initVarUpdater(BeanGoalContext goal, ParameterSpec varUpdater) {
    DtoContext.GoalContext context = goal.context();
    if (goal.context.lifecycle == REUSE_INSTANCES) {
      FieldSpec cache = context.cache.get();
      ParameterSpec varContext = parameterSpec(context.generatedType, "context");
      FieldSpec updaterField = Updater.cacheField(goal);
      return CodeBlock.builder()
          .addStatement("$T $N = $N.get()", varContext.type, varContext, cache)
          .beginControlFlow("if ($N.$N._currently_in_use)", varContext, updaterField)
          .addStatement("$N.$N = new $T()", varContext, updaterField, varUpdater.type)
          .endControlFlow()
          .addStatement("$T $N = $N.$N", varUpdater.type, varUpdater, varContext, updaterField)
          .addStatement("$N.$N = new $T()", varUpdater, goal.bean(), goal.details.goalType)
          .addStatement("$N._currently_in_use = true", varUpdater)
          .build();
    }
    return statement("$T $N = new $T()", varUpdater.type, varUpdater, varUpdater.type);
  }

  private static ParameterSpec updaterInstance(BeanGoalContext goal) {
    ClassName updaterType = implType(goal);
    return parameterSpec(updaterType, "updater");
  }
  private GeneratorB() {
    throw new UnsupportedOperationException("no instances");
  }
}
