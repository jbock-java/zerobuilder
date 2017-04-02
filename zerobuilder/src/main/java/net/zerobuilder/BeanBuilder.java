package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Combination of {@link Builder} and {@link Updater}.
 * This annotation goes on top of a bean class declaration.
 */
@Retention(SOURCE)
@Target(TYPE)
public @interface BeanBuilder {
}
