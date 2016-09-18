package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * An optional annotation on {@link Goal} parameters.
 * </p><p>
 * In the case of a bean goal, this annotation goes on the getters of the target type.
 * By default, bean steps are in alphabetic order.
 * </p>
 *
 * @see Goal
 * @see Builders
 */
@Retention(SOURCE)
@Target({PARAMETER, METHOD})
public @interface Step {

  /**
   * <p>Overrides the default position in the generated chain of builder steps
   * for this parameter.</p>
   *
   * @return the position that this step should occupy in the step chain
   */
  int value();

}
