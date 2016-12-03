package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * Marks a &quot;getter&quot; method as ignored.
 * </p>
 */
@Retention(SOURCE)
@Target({METHOD})
public @interface BeanIgnore {
}
