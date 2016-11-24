package net.zerobuilder.modules.builder;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularStep;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;
import net.zerobuilder.compiler.generate.DtoStep;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoParameter.parameterName;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.simpleGoalCases;
import static net.zerobuilder.compiler.generate.DtoStep.always;
import static net.zerobuilder.compiler.generate.ZeroUtil.concat;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.nullCheck;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.modules.builder.Builder.nextType;

final class Step {

  static Function<AbstractRegularStep, TypeSpec> regularStepInterface(SimpleRegularGoalContext goal) {
    return step -> interfaceBuilder(step.thisType)
        .addMethod(regularStepMethod(step, goal))
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec regularStepMethod(AbstractRegularStep step, SimpleGoal goal) {
    AbstractParameter parameter = step.abstractParameter();
    String name = parameterName.apply(parameter);
    TypeName type = parameter.type;
    List<TypeName> thrownTypes = step.declaredExceptions();
    if (step.isLast()) {
      thrownTypes = concat(Step.thrownTypes.apply(goal), thrownTypes);
    }
    return methodBuilder(name)
        .returns(nextType(step))
        .addParameter(parameterSpec(type, name))
        .addExceptions(thrownTypes)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  private static final Function<SimpleGoal, List<TypeName>> thrownTypes =
      simpleGoalCases(
          regular -> regular.thrownTypes,
          bean -> Collections.emptyList());

  static final Function<DtoStep.AbstractStep, CodeBlock> nullCheck
      = always(step -> {
    AbstractParameter parameter = step.abstractParameter();
    if (!parameter.nullPolicy.check() || parameter.type.isPrimitive()) {
      return emptyCodeBlock;
    }
    String name = parameterName.apply(parameter);
    return nullCheck(name, name);
  });

  static Function<DtoRegularStep.AbstractRegularStep, TypeSpec> asStepInterface(SimpleRegularGoalContext goal) {
    return regularStepInterface(goal);
  }

  private Step() {
    throw new UnsupportedOperationException("no instances");
  }
}
