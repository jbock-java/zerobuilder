package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * An optional annotation on parameters.
 * </p>
 */
@Retention(SOURCE)
@Target(PARAMETER)
public @interface Step {

  /**
   * <p>Overrides the default position in the generated chain of builder steps
   * for this parameter.</p>
   *
   * @return The desired position of this step in the builder chain.
   */
  int value();
}
