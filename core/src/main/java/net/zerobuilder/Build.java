package net.zerobuilder;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * Declares that a class named {@code some.package.MyObjectBuilders} should be generated,
 * where {@code some.package.MyObject} is the name of the class that carries the {@link Build} annotation.
 * The generated builders will invoke the elements (methods or constructors)
 * carrying the {@link Goal} annotation. These elements must at least be package visible.
 * </p><p>
 * If the the {@link Goal} annotation is not present on any executable element of the {@link Build} annotated class,
 * the annotation processor tries to guess a <em>single</em> goal.
 * </p><p>
 * If the {@link Goal} annotation is present on a non-static method,
 * the generated {@code static someGoalBuilder} method will take a parameter of type {@code MyObject}.
 * </p><p>
 * A goal may return anything, including {@code void}.
 * </p><p>
 * Only one goal per class may have the {@link Goal#toBuilder} flag set. Additional rules apply in that case:
 * <ul>
 * <li>The goal must be a constructor, or a static method that returns the {@link Build} annotated type.</li>
 * <li>Each parameter of the goal must have a corresponding projection,
 * i.e. a corresponding non-private &quot;getter&quot;, or a non-private instance field
 * of the same type and name.</li>
 * </ul>
 * </p>
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface Build {

  /**
   * If this flag is set, the generated code will cache the builder instances in a {@link ThreadLocal}.
   */
  boolean nogc() default false;

}
