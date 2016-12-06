package net.zerobuilder.modules.builder.bean;

import com.squareup.javapoet.CodeBlock;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.NullPolicy;

import java.util.function.Function;

import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.nullCheck;

final class Step {

  static final Function<AbstractBeanParameter, CodeBlock> nullCheck =
      parameter -> {
        if (parameter.nullPolicy == NullPolicy.ALLOW || parameter.type.isPrimitive()) {
          return emptyCodeBlock;
        }
        String name = parameter.name();
        return nullCheck(name, name);
      };

  private Step() {
    throw new UnsupportedOperationException("no instances");
  }
}
