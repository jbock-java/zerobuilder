package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Marks this method or constructor as a build goal.
 * It is an error if the enclosing type doesn't carry the {@link Builder} annotation.
 *
 * @see Builder
 */
@Retention(SOURCE)
@Target({METHOD, CONSTRUCTOR})
public @interface Goal {

  /**
   * <p>
   * Overrides the default name of this build goal.
   * This is necessary if there are multiple constructor goals,
   * or multiple goals with the same return type.
   * </p><p>
   * By default, a goal is named by its return type,
   * or, if the goal is a constructor, by the enclosing class.
   * It is an error if two goals have the same name.
   * </p>
   */
  String name() default "";

  /**
   * <p>If {@code true}, declares that a {@code static toBuilder} method
   * should be generated for this goal.
   * </p><p>
   * The goal may be a constructor or static method,
   * but not an instance method.
   * </p>
   */
  boolean toBuilder() default false;
}
