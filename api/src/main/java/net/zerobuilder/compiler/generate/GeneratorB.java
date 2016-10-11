package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoalContext.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.BeanStepCases;
import net.zerobuilder.compiler.generate.DtoBeanStep.LoneGetterStep;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;

import javax.lang.model.element.Modifier;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.NullPolicy.ALLOW;
import static net.zerobuilder.compiler.generate.DtoBeanStep.asFunction;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builderImplType;
import static net.zerobuilder.compiler.generate.Generator.stepsField;
import static net.zerobuilder.compiler.generate.Generator.updaterField;
import static net.zerobuilder.compiler.generate.Updater.updaterType;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.Utilities.join;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.statement;

final class GeneratorB {

  static final Function<BeanGoalContext, BuilderMethod> goalToToBuilder
      = goal -> {
    ParameterSpec parameter = parameterSpec(goal.goal.details.goalType, goal.goal.field.name);
    String name = goal.goal.details.name;
    ParameterSpec updater = updaterInstance(goal);
    Modifier[] modifiers = goal.goal.details.goalOptions.toBuilderAccess.modifiers(STATIC);
    MethodSpec method = methodBuilder(downcase(name + "ToBuilder"))
        .addParameter(parameter)
        .returns(updaterType(goal))
        .addCode(goal.goal.steps.stream().map(nullChecks(goal)).collect(join))
        .addCode(initializeUpdater(goal, updater))
        .addCode(goal.goal.steps.stream().map(copy(goal)).collect(join))
        .addStatement("return $N", updater)
        .addModifiers(modifiers)
        .build();
    return new BuilderMethod(name, method);
  };

  private static Function<AbstractBeanStep, CodeBlock> copy(final BeanGoalContext goal) {
    return asFunction(new BeanStepCases<CodeBlock>() {
      @Override
      public CodeBlock accessorPair(AccessorPairStep step) {
        return copyRegular(goal, step);
      }
      @Override
      public CodeBlock loneGetter(LoneGetterStep step) {
        return copyCollection(goal, step);
      }
    });
  }

  private static Function<AbstractBeanStep, CodeBlock> nullChecks(final BeanGoalContext goal) {
    return asFunction(new BeanStepCases<CodeBlock>() {
      @Override
      public CodeBlock accessorPair(AccessorPairStep step) {
        return step.accessorPair.nullPolicy == ALLOW
            ? emptyCodeBlock
            : nullCheck(goal, step.accessorPair);
      }
      @Override
      public CodeBlock loneGetter(LoneGetterStep step) {
        return nullCheck(goal, step.loneGetter);
      }
    });
  }

  private static CodeBlock copyCollection(BeanGoalContext goal, LoneGetterStep step) {
    ParameterSpec parameter = parameterSpec(goal.goal.details.goalType, goal.goal.field.name);
    ParameterSpec iterationVar = step.loneGetter.iterationVar(parameter);
    return CodeBlock.builder()
        .beginControlFlow("for ($T $N : $N.$N())",
            iterationVar.type, iterationVar, parameter,
            step.loneGetter.getter)
        .addStatement("$N.$N.$N().add($N)", updaterInstance(goal),
            downcase(goal.goal.details.goalType.simpleName()),
            step.loneGetter.getter,
            iterationVar)
        .endControlFlow()
        .build();
  }

  private static CodeBlock copyRegular(BeanGoalContext goal, AccessorPairStep step) {
    ParameterSpec parameter = parameterSpec(goal.goal.details.goalType, goal.goal.field.name);
    ParameterSpec updater = updaterInstance(goal);
    return CodeBlock.builder()
        .addStatement("$N.$N.$L($N.$N())", updater,
            goal.goal.field,
            step.accessorPair.setterName(),
            parameter,
            step.accessorPair.getter)
        .build();
  }

  private static CodeBlock nullCheck(BeanGoalContext goal, AbstractBeanParameter beanParameter) {
    ParameterSpec parameter = parameterSpec(goal.goal.details.goalType, goal.goal.field.name);
    return CodeBlock.builder()
        .beginControlFlow("if ($N.$N() == null)", parameter,
            beanParameter.getter)
        .addStatement("throw new $T($S)",
            NullPointerException.class, beanParameter.name())
        .endControlFlow().build();
  }

  private static CodeBlock initializeUpdater(BeanGoalContext goal, ParameterSpec updater) {
    CodeBlock.Builder builder = CodeBlock.builder();
    if (goal.builders.lifecycle.recycle()) {
      builder.addStatement("$T $N = $N.get().$N", updater.type, updater,
          goal.builders.cache, updaterField(goal));
    } else {
      builder.addStatement("$T $N = new $T()", updater.type, updater, updater.type);
    }
    builder.addStatement("$N.$N = new $T()", updater, goal.goal.field, goal.goal.details.goalType);
    return builder.build();
  }

  private static ParameterSpec updaterInstance(BeanGoalContext goal) {
    ClassName updaterType = updaterType(goal);
    return parameterSpec(updaterType, "updater");
  }

  static final Function<BeanGoalContext, BuilderMethod> goalToBuilder
      = goal -> {
    ClassName stepsType = builderImplType(goal);
    String name = goal.goal.details.name;
    MethodSpec.Builder method = methodBuilder(name + "Builder")
        .returns(goal.goal.steps.get(0).thisType)
        .addModifiers(goal.goal.details.goalOptions.builderAccess.modifiers(STATIC));
    String steps = downcase(stepsType.simpleName());
    method.addCode(goal.builders.lifecycle.recycle()
        ? statement("$T $N = $N.get().$N", stepsType, steps, goal.builders.cache, stepsField(goal))
        : statement("$T $N = new $T()", stepsType, steps, stepsType));
    MethodSpec methodSpec = method.addStatement("$N.$N = new $T()", steps,
        downcase(goal.goal.details.goalType.simpleName()), goal.goal.details.goalType)
        .addStatement("return $N", steps)
        .build();
    return new BuilderMethod(name, methodSpec);
  };

  private GeneratorB() {
    throw new UnsupportedOperationException("no instances");
  }
}
