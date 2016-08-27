package com.kaputtjars.isobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Request for a builder class to be generated, which invokes the
 * target (method or constructor) in its {@code build()} method.
 * <ul>
 * <li>This annotation may only be used on constructors and <i>static</i> methods.</li>
 * <li>The target (method or constructor) may not have less than 2 parameters.</li>
 * <li>The target (method or constructor) may not be private.</li>
 * <li>The class containing the target (method or constructor) must be a top level class,
 * or a <i>static</i> inner class.</li>
 * <li>The class containing the target (method or constructor) may not be private.</li>
 * </ul>
 */
@Retention(RUNTIME)
@Target({METHOD, CONSTRUCTOR})
public @interface Builder {
}
