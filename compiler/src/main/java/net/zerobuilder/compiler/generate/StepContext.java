package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.analyse.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import static net.zerobuilder.compiler.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.Utilities.nullCheck;
import static net.zerobuilder.compiler.analyse.DtoParameter.parameterName;
import static net.zerobuilder.compiler.generate.DtoStep.always;
import static net.zerobuilder.compiler.generate.DtoStep.asFunction;
import static net.zerobuilder.compiler.generate.DtoStep.stepCases;
import static net.zerobuilder.compiler.generate.DtoStep.validParameter;
import static net.zerobuilder.compiler.generate.StepContextB.beanStepInterface;
import static net.zerobuilder.compiler.generate.StepContextV.regularStepInterface;

final class StepContext {

  static final Function<AbstractStep, CodeBlock> nullCheck
      = always(new Function<AbstractStep, CodeBlock>() {
    @Override
    public CodeBlock apply(AbstractStep context) {
      AbstractParameter parameter = context.accept(validParameter);
      if (!parameter.nonNull || parameter.type.isPrimitive()) {
        return emptyCodeBlock;
      }
      String name = parameter.acceptParameter(parameterName);
      return nullCheck(name, name);
    }
  });

  static final Function<AbstractStep, TypeSpec> asStepInterface
      = asFunction(stepCases(regularStepInterface, beanStepInterface));

  private StepContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
