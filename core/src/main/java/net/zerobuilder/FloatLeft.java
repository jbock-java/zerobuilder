package net.zerobuilder;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>Overrides the default position in the sequence of builder steps for this parameter.
 * The step of a parameter that is annotated with {@link FloatLeft}
 * always comes before any steps of parameters that don't have this annotation.
 * </p><p>
 * It is allowed not to specify a value, or to specify the same value on two different parameters.
 * In that case, the original sequence of parameters will be respected.
 * </P>
 */
@Documented
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface FloatLeft {

  int value() default 0;

}
