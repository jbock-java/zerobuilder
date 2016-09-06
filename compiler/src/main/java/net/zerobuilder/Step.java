package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;

@Retention(SOURCE)
@Target(PARAMETER)
public @interface Step {

  /**
   * <p>Override the position in the step sequence for this parameter.
   * </p>
   */
  int value();

}
