package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import java.util.function.Function;

import static net.zerobuilder.compiler.generate.DtoParameter.parameterName;
import static net.zerobuilder.compiler.generate.DtoStep.abstractParameter;
import static net.zerobuilder.compiler.generate.DtoStep.always;
import static net.zerobuilder.compiler.generate.DtoStep.asFunction;
import static net.zerobuilder.compiler.generate.DtoStep.stepCases;
import static net.zerobuilder.compiler.generate.StepB.beanStepInterface;
import static net.zerobuilder.compiler.generate.StepV.regularStepInterface;
import static net.zerobuilder.compiler.generate.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.Utilities.nullCheck;

final class Step {

  static final Function<AbstractStep, CodeBlock> nullCheck
      = always(step -> {
        AbstractParameter parameter = abstractParameter.apply(step);
        if (!parameter.nullPolicy.check() || parameter.type.isPrimitive()) {
          return emptyCodeBlock;
        }
        String name = parameterName.apply(parameter);
        return nullCheck(name, name);
      });

  static final Function<AbstractStep, TypeSpec> asStepInterface
      = asFunction(stepCases(regularStepInterface, beanStepInterface));

  private Step() {
    throw new UnsupportedOperationException("no instances");
  }
}
