package net.zerobuilder;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * Declares that the builder pattern code should be generated. The generated builder will
 * invoke the element (method or constructor) carrying the {@link Goal} annotation.
 * </p><p>
 * {@link Goal} cannot appear more than once per class, except in {@code static} inner classes.
 * If it is missing, the annotation processor tries to guess the goal,
 * by checking for suitable constructors first, then static methods,
 * and finally instance methods.
 * </p><p>
 * In the case of instance methods, the generated
 * {@code static builder()} (and optionally {@code static toBuilder()}) methods
 * will take an instance parameter respectively.
 * </p><p>
 * The goal may return anything, including {@code void}. The name of the generated class
 * however will always be {@code XXXBuilder}, where {@code XXX} is the name of the class
 * that carries the {@code Build} annotation.
 * </p>
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface Build {

  boolean nogc() default false;

  @Retention(SOURCE)
  @Target({METHOD, CONSTRUCTOR})
  @interface Goal {

    String value() default "";
    boolean toBuilder() default false;
  }

}
