package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * Marks this method, constructor or field as a build goal.
 * It is an error if the enclosing type doesn't carry the {@link Builders} annotation.
 * </p><p>
 * If this annotation appears on a constructor or field, then the annotated element may not be {@code private}.
 * If it appears on a field, the following rules apply:
 * </p>
 * <ul><li>
 * The <em>type</em> of the field is marked as a build goal. The field <em>value</em> is not considered and may be {@code null}.
 * </li><li>
 * The field may or may not have any modifiers, including {@code static}, {@code final} and {@code private}.
 * </li><li>
 * There will be one generated step for each <em>setter</em> of the target type.
 * </li><li>
 * The target type must have a public no-argument constructor.
 * </li></ul>
 *
 * @see Builders
 * @see Step
 */
@Retention(SOURCE)
@Target({METHOD, CONSTRUCTOR, FIELD})
public @interface Goal {

  /**
   * <p>
   * Overrides the default name of this build goal.
   * This is necessary if there are multiple goals that would normally have the same name.
   * </p><p>
   * By default, a method goal is named by its return type,
   * a constructor by its enclosing class, and a field goal by its type.
   * It is an error if two goals have the same name.
   * </p>
   *
   * @return goal name, or empty string to indicate that the default goal name should be used
   */
  String name() default "";

  /**
   * <p>If {@code true}, declares that a {@code static toBuilder} method
   * should be generated for this goal.
   * </p><p>
   * In this case, the goal may be a constructor, field or static method,
   * but not an instance method.
   * </p>
   *
   * @return toBuilder flag, defaults to {@code false}
   */
  boolean toBuilder() default false;
}
