package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * This annotation triggers the code generation for a class named {@code *Builder},
 * where {@code *} is the name of the annotated class.
 *
 * <p>The annotation is intended for Java records,
 * but it can also be used on other data classes,
 * as long as they have exactly one constructor, which must not be private,
 * and one &quot;projection&quot; per constructor parameter.
 *
 * <p>Projections can be non-private record-style accessor methods
 * or non-private fields. The projections must match the names and types
 * of the constructor parameters.
 */
@Retention(SOURCE)
@Target(TYPE)
public @interface RecordBuilder {

  /**
   * This property determines the class visibility
   * of the generated builder class.
   * If {@code visibility == AUTO}, the builder class
   * inherits the visibility from the annotated class.
   */
  Visibility visibility() default Visibility.AUTO;

  /**
   * If this is {@code true}, the updater class
   * and the single-argument {@code builder(X)} method
   * are <em>not</em> created.
   */
  boolean createOnly() default false;

  /**
   * If this is {@code true}, the telescoping builder
   * class and interfaces, and the no-argument
   * {@code builder()} method are <em>not</em> created.
   */
  boolean updateOnly() default false;

}
