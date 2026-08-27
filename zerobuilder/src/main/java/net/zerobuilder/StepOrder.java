package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Determine step position in the generated telescoping builder.
 */
@Retention(SOURCE)
@Target(PARAMETER)
public @interface StepOrder {

  /**
   * @return desired position
   */
  int value();
}
