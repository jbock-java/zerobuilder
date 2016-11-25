package net.zerobuilder.modules.builder.bean;

import com.squareup.javapoet.CodeBlock;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;

import java.util.function.Function;

import static net.zerobuilder.compiler.generate.DtoParameter.parameterName;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.nullCheck;

final class Step {

  static final Function<AbstractBeanParameter, CodeBlock> nullCheck =
      parameter -> {
        if (!parameter.nullPolicy.check() || parameter.type.isPrimitive()) {
          return emptyCodeBlock;
        }
        String name = parameterName.apply(parameter);
        return nullCheck(name, name);
      };

  private Step() {
    throw new UnsupportedOperationException("no instances");
  }
}
