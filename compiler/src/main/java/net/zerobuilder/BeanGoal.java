package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * Marks the <em>type</em> of this field as a build goal.
 * The field <em>value</em> is not considered and may be {@code null}.
 * </p><p>
 * The field must be {@code static} and may be {@code private}.
 * </p><p>
 * This goal will always attempt to generate a {@code toBuilder} method.
 * </p><p>
 * There will be one generated step for each <em>setter</em> of the target type.
 * </p><p>
 * It is an error if the enclosing type doesn't carry the {@link Builder} annotation.
 * </p>
 *
 * @see Builder
 */
@Retention(SOURCE)
@Target({FIELD})
public @interface BeanGoal {

  /**
   * <p>
   * Overrides the default name of this build goal.
   * This can be used to resolve goal name collisions.
   * </p>
   */
  String name() default "";

}
