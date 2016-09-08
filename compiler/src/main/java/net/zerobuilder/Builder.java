package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * Declares that a class named {@code some.package.MyObjectBuilders} should be generated,
 * where {@code some.package.MyObject} is the name of the class that carries the {@link Builder} annotation.
 * The generated builders' job will be to invoke those methods or constructors
 * which carry the {@link Goal} annotation. These &quot;goals&quot; may not be {@code private}.
 * </p><p>
 * If the {@link Goal} annotation is present on a non-static method,
 * the generated {@code static someGoalBuilder} method will take a parameter of type {@code MyObject}.
 * </p><p>
 * A goal method may return anything, including {@code void}.
 * </p>
 *
 * @see Goal
 * @see Step
 */
@Retention(SOURCE)
@Target(TYPE)
public @interface Builder {

  /**
   * When this flag is set to {@code true},
   * the generated code will cache builder instances in a {@link ThreadLocal}.
   */
  boolean recycle() default false;

}
