package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>
 * Marks a &quot;getter&quot; method as ignored.
 * This may be necessary a method that looks like a &quot;getter&quot;
 * has no corresponding &quot;setter&quot;.
 * </p><p>
 * <em>Note:</em> Only applies to &quot;bean goals&quot;. See {@link Goal}
 * </p><p>
 * <em>Note:</em> According to the &quot;bean standard&quot;,
 * each property is defined by an <em>accessor pair</em>.
 * However, some tools may generate code that violates this rule,
 * when the property is of a subtype of {@link java.util.Collection}.
 * The code generator <em>should</em> handle this special case correctly.
 * Ergo, ignoring a &quot;lone getter&quot; should be unnecessary,
 * <em>if</em> it returns a subtype of {@link java.util.Collection}.
 * </p>
 */
@Retention(SOURCE)
@Target({METHOD})
public @interface Ignore {
}
