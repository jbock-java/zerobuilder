package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * Override the default goal name. The goal name determines the name of the generated static method,
 * i.e. the {@code x} in the static method {@code xBuilder} or {@code xUpdater}.
 * </p>
 * <p>
 * By default, constructor goals are named by the constructor type, and method goals are named by the method's
 * return type.
 * </p>
 */
@Retention(SOURCE)
@Target({METHOD, CONSTRUCTOR})
public @interface GoalName {
  String value();
}
