package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import static net.zerobuilder.NullPolicy.DEFAULT;

/**
 * <p>
 * An optional annotation on {@link Goal} parameters.
 * </p><p>
 * In the case of a bean goal, this annotation goes on a <em>getter</em> of the bean.
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
   * @return The desired position of this step in the builder chain.
   * Negative values are ignored.
   */
  int value() default -1;

  NullPolicy nullPolicy() default DEFAULT;

}
