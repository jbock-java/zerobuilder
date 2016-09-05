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
 * The generated builders' job will be to invoke those methods or constructors
 * which carry the {@link Goal} annotation. These &quot;goals&quot; may not be {@code private}.
 * </p><p>
 * If the {@link Goal} annotation is present on a non-static method,
 * the generated {@code static someGoalBuilder} method will take a parameter of type {@code MyObject}.
 * </p><p>
 * A goal method may return anything, including {@code void}.
 * </p><p>
 * Only one goal per {@link Build} may have the {@link Goal#toBuilder} flag set to {@code true}.
 * Additional rules apply in this case:
 * <ul>
 * <li>The goal must either be a constructor,
 * or a static method that returns the type which carries the {@link Build} annotation.</li>
 * <li>Each parameter of the goal must have a corresponding projection,
 * i.e. a non-private &quot;getter&quot; or instance field of the same type and name.</li>
 * </ul>
 * </p>
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface Build {

  /**
   * When this flag is set to {@code true},
   * the generated code will cache builder instances in a {@link ThreadLocal}.
   */
  boolean nogc() default false;

}
