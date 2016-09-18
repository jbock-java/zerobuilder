package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * Marks this method, constructor or class as a build goal.
 * </p><p>
 * If this annotation appears on a constructor or field, then the annotated element may not be {@code private}.
 * </p><p>
 * If this annotation is present on a non-static method,
 * the generated {@code static someGoalBuilder} method will take a parameter of type {@code MyObject}.
 * </p><p>
 * If this annotation appears on a class, it marks the class as a bean goal.
 * In this case, the class must also have the {@link Builders} annotation.
 * It must also have public accessor pairs and a public default constructor.
 * </p>
 *
 * @see Builders
 * @see Step
 */
@Retention(SOURCE)
@Target({METHOD, CONSTRUCTOR, TYPE})
public @interface Goal {

  /**
   * <p>
   * Overrides the default name of this build goal.
   * This is necessary if there are multiple goals that would normally have the same name.
   * </p><p>
   * By default, a method goal is named by its return type,
   * a constructor by the type it creates, and a "bean" goal by the bean class.
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

  /**
   * <p>Set to {@code false} if no {@code static builder} method
   * should be generated for this goal.</p>
   *
   * @return builder flag, defaults to {@code true}
   */
  boolean builder() default true;

}
