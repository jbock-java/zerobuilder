package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoalContext.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.LoneGetterStep;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;

import javax.lang.model.element.Modifier;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.NullPolicy.ALLOW;
import static net.zerobuilder.compiler.generate.DtoBeanStep.beanStepCases;
import static net.zerobuilder.compiler.generate.DtoBuildersContext.BuilderLifecycle.REUSE_INSTANCES;
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
    String name = goal.goal.details.name;
    ClassName type = goal.goal.details.goalType;
    ParameterSpec updater = updaterInstance(goal);
    Modifier[] modifiers = goal.goal.details.goalOptions.toBuilderAccess.modifiers(STATIC);
    MethodSpec method = methodBuilder(downcase(name + "ToBuilder"))
        .addParameter(parameterSpec(type, downcase(type.simpleName())))
        .returns(updaterType(goal))
        .addCode(goal.goal.steps.stream().map(nullChecks(goal)).collect(join))
        .addCode(initializeUpdater(goal, updater))
        .addCode(goal.goal.steps.stream().map(copy(goal)).collect(join))
        .addStatement("return $N", updater)
        .addModifiers(modifiers)
        .build();
    return new BuilderMethod(name, method);
  };

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
            goal.goal.bean(),
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
    FieldSpec cache = goal.builders.cache;
    ClassName type = goal.goal.details.goalType;
    builder.add(goal.builders.lifecycle == REUSE_INSTANCES
        ? statement("$T $N = $N.get().$N", updater.type, updater, cache, updaterField(goal))
        : statement("$T $N = new $T()", updater.type, updater, updater.type));
    builder.addStatement("$N.$N = new $T()", updater, goal.goal.bean(), type);
    return builder.build();
  }

  private static ParameterSpec updaterInstance(BeanGoalContext goal) {
    ClassName updaterType = updaterType(goal);
    return parameterSpec(updaterType, "updater");
  }

  static final Function<BeanGoalContext, BuilderMethod> goalToBuilder
      = goal -> {
    ClassName builderType = builderImplType(goal);
    String name = goal.goal.details.name;
    String builder = downcase(builderType.simpleName());
    ClassName type = goal.goal.details.goalType;
    FieldSpec field = goal.goal.bean();
    MethodSpec method = methodBuilder(name + "Builder")
        .returns(goal.goal.steps.get(0).thisType)
        .addModifiers(goal.goal.details.goalOptions.builderAccess.modifiers(STATIC))
        .addCode(goal.builders.lifecycle == REUSE_INSTANCES
            ? statement("$T $N = $N.get().$N", builderType, builder, goal.builders.cache, stepsField(goal))
            : statement("$T $N = new $T()", builderType, builder, builderType))
        .addStatement("$N.$N = new $T()", builder, field, type)
        .addStatement("return $N", builder)
        .build();
    return new BuilderMethod(name, method);
  };

  private GeneratorB() {
    throw new UnsupportedOperationException("no instances");
  }
}
