package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Declares that a builder class should be generated. The builder's goal is to invoke
 * the element (method or constructor) carrying the {@link Via} annotation.
 * <ul>
 * <li>The class must be a top level class, or a <i>static</i> inner class.</li>
 * <li>The class may not be private.</li>
 * <li>Only one element (static method or constructor) may carry the {@link Via} annotation.</li>
 * <li>For each argument of the element carrying the {@link Via} annotation, the class must declare
 * a parameterless method which has the same name as the argument, and returns the argument's type.
 * <li>The {@link Via} annotation may be used on constructors or <i>static</i> methods
 * that return the type of their enclosing class.</li>
 * <li>The element carrying the {@link Via} annotation may not have less than 2 parameters.</li>
 * <li>The element carrying the {@link Via} annotation may not be private.</li>
 * </ul>
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface Build {

  boolean toBuilder() default false;
  boolean garbageless() default true;

  @Retention(SOURCE)
  @Target({METHOD, CONSTRUCTOR})
  @interface Via {
  }

}
