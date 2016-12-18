package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Request builder
 */
@Retention(SOURCE)
@Target({METHOD, CONSTRUCTOR})
public @interface Builder {

  Style style() default Style.AUTO;
}
