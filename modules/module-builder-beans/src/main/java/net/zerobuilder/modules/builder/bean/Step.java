package net.zerobuilder.modules.builder.bean;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import java.util.function.Function;

import static net.zerobuilder.compiler.generate.DtoParameter.parameterName;
import static net.zerobuilder.compiler.generate.DtoStep.always;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.nullCheck;
import static net.zerobuilder.modules.builder.bean.BeanStep.beanStepInterface;

final class Step {

  static final Function<AbstractStep, CodeBlock> nullCheck
      = always(step -> {
    AbstractParameter parameter = step.abstractParameter();
    if (!parameter.nullPolicy.check() || parameter.type.isPrimitive()) {
      return emptyCodeBlock;
    }
    String name = parameterName.apply(parameter);
    return nullCheck(name, name);
  });

  static Function<AbstractBeanStep, TypeSpec> asStepInterface() {
    return beanStepInterface;
  }

  private Step() {
    throw new UnsupportedOperationException("no instances");
  }
}
