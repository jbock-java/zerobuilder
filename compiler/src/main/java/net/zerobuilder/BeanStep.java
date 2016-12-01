package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import static net.zerobuilder.NullPolicy.DEFAULT;

@Retention(SOURCE)
@Target({METHOD})
public @interface BeanStep {

  int value() default -1;
  NullPolicy nullPolicy() default DEFAULT;
}
