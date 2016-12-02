package net.zerobuilder;

import net.zerobuilder.compiler.generate.NullPolicy;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import static net.zerobuilder.AccessLevel.PUBLIC;
import static net.zerobuilder.compiler.generate.NullPolicy.ALLOW;

@Retention(SOURCE)
@Target({METHOD, CONSTRUCTOR})
public @interface Updater {

  boolean recycle() default false;
  NullPolicy nullPolicy() default ALLOW;
  AccessLevel access() default PUBLIC;

}
