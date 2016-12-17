package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * Null policy for the getter.
 * </p>
 * <p>
 * The annotated getter will never return null, under one condition:
 * This only applies if the bean was created or updated using the generated {@code xBuilder} and {@code xUpdater}
 * methods, and its setters never invoked directly.
 * </p>
 */
@Retention(SOURCE)
@Target(METHOD)
public @interface NotNullGetter {
}
