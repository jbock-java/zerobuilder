package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Override default goal name
 */
@Retention(SOURCE)
@Target({METHOD, CONSTRUCTOR})
public @interface GoalName {
  String value();
}
