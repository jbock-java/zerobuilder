package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * Marks a getter method as ignored. Only applies to bean goals.
 * </p>
 *
 * @see BeanBuilder
 */
@Retention(SOURCE)
@Target({METHOD})
public @interface IgnoreGetter {
}
