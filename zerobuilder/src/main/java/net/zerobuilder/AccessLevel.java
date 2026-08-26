package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Controls the visibility of the generated builder.
 */
@Retention(SOURCE)
@Target({CONSTRUCTOR})
public @interface AccessLevel {

  Visibility value();
}
