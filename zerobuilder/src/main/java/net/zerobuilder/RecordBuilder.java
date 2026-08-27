package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * This annotation triggers the code generation for a class named {@code *Builder},
 * which implements the telescoping builder pattern.
 *
 * <p>The annotation works is intended for java records,
 * but it can also be used on other data classes,
 * as long as they have only one constructor.
 */
@Retention(SOURCE)
@Target(TYPE)
public @interface RecordBuilder {

  /**
   * @return desired visibility of the builder class.
   * If this is {@code AUTO}, the builder
   * inherits the visibility of its target class.
   */
  Visibility visibility() default Visibility.AUTO;
}
