package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Request updater
 */
@Retention(SOURCE)
@Target({CONSTRUCTOR})
public @interface Updater {
}
