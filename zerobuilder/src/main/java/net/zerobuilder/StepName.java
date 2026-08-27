package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Override the method name of the generated step and updater method.
 */
@Retention(SOURCE)
@Target(PARAMETER)
public @interface StepName {

  /**
   * @return desired name
   */
  String value();
}
