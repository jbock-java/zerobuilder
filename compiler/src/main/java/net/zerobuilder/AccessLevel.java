package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Access level of generated static method.
 *
 * @see Builder
 * @see Updater
 */
@Retention(SOURCE)
@Target({METHOD, CONSTRUCTOR})
public @interface AccessLevel {

  Level value() default Level.PUBLIC;
}
