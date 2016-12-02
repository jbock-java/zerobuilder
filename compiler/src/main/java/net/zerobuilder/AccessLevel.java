package net.zerobuilder;

import net.zerobuilder.compiler.generate.Access;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

@Retention(SOURCE)
@Target({METHOD, CONSTRUCTOR})
public @interface AccessLevel {
  Access value() default Access.PUBLIC;
}
