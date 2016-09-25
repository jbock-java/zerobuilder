package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.analyse.DtoShared.ValidParameter;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;
import net.zerobuilder.compiler.generate.DtoStep.BeanStep;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;
import net.zerobuilder.compiler.generate.DtoStep.StepCases;

import static net.zerobuilder.compiler.Utilities.iterationVar;
import static net.zerobuilder.compiler.Utilities.nullCheck;
import static net.zerobuilder.compiler.generate.DtoStep.always;
import static net.zerobuilder.compiler.generate.DtoStep.stepCases;
import static net.zerobuilder.compiler.generate.StepContextB.beanStepInterface;
import static net.zerobuilder.compiler.generate.StepContextV.regularStepInterface;

public final class StepContext {

  static final StepCases<ImmutableList<TypeName>> declaredExceptions
      = new StepCases<ImmutableList<TypeName>>() {
    @Override
    public ImmutableList<TypeName> regularStep(RegularStep step) {
      return step.declaredExceptions;
    }
    @Override
    public ImmutableList<TypeName> beanStep(BeanStep step) {
      return ImmutableList.of();
    }
  };

  static final StepCases<ValidParameter> validParameter
      = new StepCases<ValidParameter>() {
    @Override
    public ValidParameter regularStep(RegularStep step) {
      return step.validParameter;
    }
    @Override
    public ValidParameter beanStep(BeanStep step) {
      return step.validParameter;
    }
  };

  static final StepCases<CodeBlock> maybeNullCheck
      = always(new Function<AbstractStep, CodeBlock>() {
    @Override
    public CodeBlock apply(AbstractStep context) {
      ValidParameter parameter = context.accept(validParameter);
      if (!parameter.nonNull || parameter.type.isPrimitive()) {
        return CodeBlock.of("");
      }
      return nullCheck(parameter.name, parameter.name);
    }
  });

  static final StepCases<CodeBlock> maybeIterationNullCheck
      = always(new Function<AbstractStep, CodeBlock>() {
    @Override
    public CodeBlock apply(AbstractStep context) {
      ValidParameter parameter = context.accept(validParameter);
      if (!parameter.nonNull || parameter.type.isPrimitive()) {
        return CodeBlock.of("");
      }
      return nullCheck(iterationVar, parameter.name + " (element)");
    }
  });

  static <R> Function<AbstractStep, R> asFunction(final StepCases<R> cases) {
    return new Function<AbstractStep, R>() {
      @Override
      public R apply(AbstractStep abstractStep) {
        return abstractStep.accept(cases);
      }
    };
  }

  static final Function<AbstractStep, TypeSpec> asStepInterface
      = asFunction(stepCases(regularStepInterface, beanStepInterface));

  private StepContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
