package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * Declares that a class named {@code some.package.MyObjectBuilders} should be generated,
 * where {@code some.package.MyObject} is the name of the class that carries the {@link Builders} annotation.
 * The generated builders' job will be to invoke those methods or constructors
 * which carry the {@link Goal} annotation. These &quot;goals&quot; may not be {@code private}.
 * </p><p>
 * When the {@code recycle} flag is set to {@code true},
 * the generated code will cache builder instances in a {@link ThreadLocal}.
 * </p>
 *
 * @see Goal
 * @see Step
 */
@Retention(SOURCE)
@Target(TYPE)
public @interface Builders {

  boolean recycle() default false;

}
