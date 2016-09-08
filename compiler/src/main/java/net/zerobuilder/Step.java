package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * An optional annotation on {@link Goal} parameters.
 *
 * @see Goal
 * @see Builder
 */
@Retention(SOURCE)
@Target(PARAMETER)
public @interface Step {

  /**
   * <p>Overrides the default position in the generated sequence of steps
   * for this parameter.</p>
   */
  int value();

}
