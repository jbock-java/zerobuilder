package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * Mark a "getter" as ignored.
 * This may be necessary if a POJO has a getter without a corresponding setter.
 * </p><p>
 * <em>Note:</em> if the "setterless getter" returns a mutable collection,
 * ignoring it may not be necessary.
 * </p>
 *
 * @see Goal
 * @see Builders
 */
@Retention(SOURCE)
@Target({METHOD})
public @interface Ignore {
}
