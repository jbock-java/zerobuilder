package net.zerobuilder;

import net.zerobuilder.compiler.generate.NullPolicy;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import static net.zerobuilder.compiler.generate.NullPolicy.DEFAULT;

@Retention(SOURCE)
@Target({METHOD})
public @interface BeanStep {

  int value() default -1;
  NullPolicy nullPolicy() default DEFAULT;
}
