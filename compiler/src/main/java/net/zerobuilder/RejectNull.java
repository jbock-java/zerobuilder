package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Request null checking
 */
@Retention(SOURCE)
@Target(PARAMETER)
public @interface RejectNull {
}
