package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import static net.zerobuilder.AccessLevel.PUBLIC;

/**
 * <p>
 * Declares that a class named {@code some.package.MyObjectBuilders} should be generated,
 * where {@code some.package.MyObject} is the name of the class that carries the {@link Builders} annotation.
 * </p><p>
 * The generated builders' job will be to invoke the methods or constructors
 * that carry the {@link Goal} annotation.
 * </p>
 *
 * @see Goal
 * @see Step
 */
@Retention(SOURCE)
@Target(TYPE)
public @interface Builders {

  /**
   * When the {@code recycle} flag is set to {@code true},
   * the generated code will cache builder instances in a {@link ThreadLocal}.
   *
   * @return recycle flag
   */
  boolean recycle() default false;

  /**
   * <p>Sets the default access level of the generated static methods.
   * If necessary, this can be overridden on the goal level.</p>
   *
   * @return default access level
   * @see Goal#builderAccess()
   * @see Goal#toBuilderAccess()
   */
  AccessLevel access() default PUBLIC;

}
