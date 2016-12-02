package net.zerobuilder;

import net.zerobuilder.compiler.generate.NullPolicy;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import static net.zerobuilder.AccessLevel.UNSPECIFIED;
import static net.zerobuilder.compiler.generate.NullPolicy.ALLOW;

@Retention(SOURCE)
@Target({TYPE})
public @interface BeanBuilder {

  boolean recycle() default false;
  NullPolicy nullPolicy() default ALLOW;
  AccessLevel access() default UNSPECIFIED;
}
