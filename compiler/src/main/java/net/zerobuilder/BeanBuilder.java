package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Request builder + updater
 */
@Retention(SOURCE)
@Target({TYPE})
public @interface BeanBuilder {
}
