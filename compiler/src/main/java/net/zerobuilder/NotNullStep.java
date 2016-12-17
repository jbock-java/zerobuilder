package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * Request null checking in the generated builders for this parameter.
 * </p>
 * <p>
 * Please note that null checking will only happen if the annotated method or constructor
 * is invoked via the generated builder or updater.
 * </p>
 */
@Retention(SOURCE)
@Target(PARAMETER)
public @interface NotNullStep {
}
