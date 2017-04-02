package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Request builder / updater reuse
 */
@Retention(SOURCE)
@Target({METHOD, CONSTRUCTOR})
public @interface Recycle {
}
