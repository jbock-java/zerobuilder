package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import net.zerobuilder.compiler.analyse.DtoBeanParameter.ValidBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanGoalContext.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoStep.BeanStepCases;
import net.zerobuilder.compiler.generate.DtoStep.LoneGetterStep;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.Utilities.statement;
import static net.zerobuilder.compiler.analyse.DtoBeanParameter.beanStepName;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builderImplName;
import static net.zerobuilder.compiler.generate.Generator.stepsField;
import static net.zerobuilder.compiler.generate.Generator.updaterField;

final class GeneratorB {

  static final Function<BeanGoalContext, MethodSpec> goalToToBuilder
      = new Function<BeanGoalContext, MethodSpec>() {
    @Override
    public MethodSpec apply(BeanGoalContext goal) {
      ParameterSpec parameter = parameterSpec(goal.goal.goalType, goal.field.name);
      MethodSpec.Builder method = methodBuilder(downcase(goal.goal.name + "ToBuilder"))
          .addParameter(parameter);
      ParameterSpec updater = updaterInstance(goal);
      method.addCode(initializeUpdater(goal, updater));
      BeanStepCases<CodeBlock> copy = copy(goal);
      for (AbstractBeanStep step : goal.steps) {
        method.addCode(step.acceptBean(copy));
      }
      method.addStatement("return $N", updater);
      return method
          .returns(goal.accept(UpdaterContext.typeName))
          .addModifiers(PUBLIC, STATIC).build();
    }
  };

  private static BeanStepCases<CodeBlock> copy(final BeanGoalContext goal) {
    return new BeanStepCases<CodeBlock>() {
      @Override
      public CodeBlock accessorPair(AccessorPairStep step) {
        return copyRegular(goal, step);
      }
      @Override
      public CodeBlock loneGetter(LoneGetterStep step) {
        return copyCollection(goal, step);
      }
    };
  }

  private static CodeBlock copyCollection(BeanGoalContext goal, LoneGetterStep step) {
    ParameterSpec parameter = parameterSpec(goal.goal.goalType, goal.field.name);
    ParameterSpec iterationVar = step.loneGetter.iterationVar(parameter);
    return CodeBlock.builder().add(nullCheck(parameter, step.loneGetter, true))
        .beginControlFlow("for ($T $N : $N.$N())",
            iterationVar.type, iterationVar, parameter,
            step.loneGetter.getter)
        .addStatement("$N.$N.$N().add($N)", updaterInstance(goal),
            downcase(goal.goal.goalType.simpleName()),
            step.loneGetter.getter,
            iterationVar)
        .endControlFlow()
        .build();
  }

  private static CodeBlock copyRegular(BeanGoalContext goal, AccessorPairStep step) {
    ParameterSpec parameter = parameterSpec(goal.goal.goalType, goal.field.name);
    ParameterSpec updater = updaterInstance(goal);
    return CodeBlock.builder()
        .add(nullCheck(parameter, step.accessorPair))
        .addStatement("$N.$N.$L($N.$N())", updater,
            goal.field,
            step.setter,
            parameter,
            step.accessorPair.getter)
        .build();
  }

  private static CodeBlock nullCheck(ParameterSpec parameter, ValidBeanParameter validParameter) {
    return nullCheck(parameter, validParameter, validParameter.nonNull);
  }

  private static CodeBlock nullCheck(ParameterSpec parameter, ValidBeanParameter validParameter, boolean nonNull) {
    if (!nonNull) {
      return emptyCodeBlock;
    }
    return CodeBlock.builder()
        .beginControlFlow("if ($N.$N() == null)", parameter,
            validParameter.getter)
        .addStatement("throw new $T($S)",
            NullPointerException.class, validParameter.accept(beanStepName))
        .endControlFlow().build();
  }

  private static CodeBlock initializeUpdater(BeanGoalContext goal, ParameterSpec updater) {
    CodeBlock.Builder builder = CodeBlock.builder();
    if (goal.builders.recycle) {
      builder.addStatement("$T $N = $N.get().$N", updater.type, updater,
          goal.builders.cache, updaterField(goal));
    } else {
      builder.addStatement("$T $N = new $T()", updater.type, updater, updater.type);
    }
    builder.addStatement("$N.$N = new $T()", updater, goal.field, goal.goal.goalType);
    return builder.build();
  }

  private static ParameterSpec updaterInstance(BeanGoalContext goal) {
    ClassName updaterType = goal.accept(UpdaterContext.typeName);
    return parameterSpec(updaterType, "updater");
  }

  static final Function<BeanGoalContext, MethodSpec> goalToBuilder
      = new Function<BeanGoalContext, MethodSpec>() {
    @Override
    public MethodSpec apply(BeanGoalContext goal) {
      ClassName stepsType = goal.accept(builderImplName);
      MethodSpec.Builder method = methodBuilder(goal.goal.name + "Builder")
          .returns(goal.steps.get(0).thisType)
          .addModifiers(PUBLIC, STATIC);
      String steps = downcase(stepsType.simpleName());
      method.addCode(goal.builders.recycle
          ? statement("$T $N = $N.get().$N", stepsType, steps, goal.builders.cache, stepsField(goal))
          : statement("$T $N = new $T()", stepsType, steps, stepsType));
      return method.addStatement("$N.$N = new $T()", steps,
          downcase(goal.goal.goalType.simpleName()), goal.goal.goalType)
          .addStatement("return $N", steps)
          .build();
    }
  };

  private GeneratorB() {
    throw new UnsupportedOperationException("no instances");
  }
}
