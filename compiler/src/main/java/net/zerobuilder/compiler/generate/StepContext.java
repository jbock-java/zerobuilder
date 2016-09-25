package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.analyse.DtoShared.ValidParameter;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;
import net.zerobuilder.compiler.generate.DtoStep.BeanStep;
import net.zerobuilder.compiler.generate.DtoStep.StepCases;

import static net.zerobuilder.compiler.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.Utilities.nullCheck;
import static net.zerobuilder.compiler.generate.DtoStep.always;
import static net.zerobuilder.compiler.generate.DtoStep.asFunction;
import static net.zerobuilder.compiler.generate.DtoStep.stepCases;
import static net.zerobuilder.compiler.generate.DtoStep.validParameter;
import static net.zerobuilder.compiler.generate.StepContextB.beanStepInterface;
import static net.zerobuilder.compiler.generate.StepContextV.regularStepInterface;

public final class StepContext {

  static final StepCases<CodeBlock> nullCheck
      = always(new Function<AbstractStep, CodeBlock>() {
    @Override
    public CodeBlock apply(AbstractStep context) {
      ValidParameter parameter = context.accept(validParameter);
      if (!parameter.nonNull || parameter.type.isPrimitive()) {
        return emptyCodeBlock;
      }
      return nullCheck(parameter.name, parameter.name);
    }
  });

  static CodeBlock iterationVarNullCheck(BeanStep step) {
    if (!step.validParameter.nonNull) {
      return emptyCodeBlock;
    }
    return nullCheck(step.validParameter.collectionType.get(),
        step.validParameter.name + " (element)");
  }

  static final Function<AbstractStep, TypeSpec> asStepInterface
      = asFunction(stepCases(regularStepInterface, beanStepInterface));

  private StepContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
