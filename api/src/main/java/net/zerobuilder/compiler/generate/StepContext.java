package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import static net.zerobuilder.compiler.generate.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.Utilities.nullCheck;
import static net.zerobuilder.compiler.generate.DtoParameter.parameterName;
import static net.zerobuilder.compiler.generate.DtoStep.abstractParameter;
import static net.zerobuilder.compiler.generate.DtoStep.always;
import static net.zerobuilder.compiler.generate.DtoStep.asFunction;
import static net.zerobuilder.compiler.generate.DtoStep.stepCases;
import static net.zerobuilder.compiler.generate.StepContextB.beanStepInterface;
import static net.zerobuilder.compiler.generate.StepContextV.regularStepInterface;

final class StepContext {

  static final Function<AbstractStep, CodeBlock> nullCheck
      = always(new Function<AbstractStep, CodeBlock>() {
    @Override
    public CodeBlock apply(AbstractStep step) {
      AbstractParameter parameter = abstractParameter.apply(step);
      if (!parameter.nonNull || parameter.type.isPrimitive()) {
        return emptyCodeBlock;
      }
      String name = parameterName.apply(parameter);
      return nullCheck(name, name);
    }
  });

  static final Function<AbstractStep, TypeSpec> asStepInterface
      = asFunction(stepCases(regularStepInterface, beanStepInterface));

  private StepContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
