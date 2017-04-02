package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Determines step position of the associated builder step.
 */
@Retention(SOURCE)
@Target(METHOD)
public @interface Getter {
  int value();
}