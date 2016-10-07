package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import static net.zerobuilder.AccessLevel.UNSPECIFIED;
import static net.zerobuilder.NullPolicy.ALLOW;
import static net.zerobuilder.NullPolicy.DEFAULT;

/**
 * <p>
 * Marks a method, constructor or class as a build goal.
 * </p><p>
 * If this annotation appears on a method or constructor,
 * the enclosing class <em>must</em> have the {@link Builders} annotation.
 * The annotated elements may not be {@code private}.
 * </p><p>
 * If this annotation appears on a class, it marks the class as a bean goal.
 * In this case, the same class <em>must</em> also have the {@link Builders} annotation,
 * and it has to comply with the the &quot;beans standard&quot;.
 * </p>
 *
 * @see Builders
 * @see Step
 */
@Retention(SOURCE)
@Target({METHOD, CONSTRUCTOR, TYPE})
public @interface Goal {

  /**
   * <p>
   * Overrides the default name of this build goal.
   * This is necessary if there are multiple goals that would normally have the same name.
   * </p><p>
   * By default, a method goal is named by its return type,
   * a constructor by the type it creates, and a "bean" goal by the bean class.
   * It is an error if two goals have the same name.
   * </p>
   *
   * @return goal name, or empty string to indicate that the default goal name should be used
   */
  String name() default "";

  /**
   * <p>If {@code true}, declares that a {@code static toBuilder} method
   * should be generated for this goal.
   * </p><p>
   * In this case, the goal may be a constructor, beanGoal or static method,
   * but not an instance method.
   * </p>
   *
   * @return toBuilder flag, defaults to {@code false}
   */
  boolean toBuilder() default false;

  /**
   * <p>Set to {@code false} if no {@code static builder} method
   * should be generated for this goal.</p>
   *
   * @return builder flag, defaults to {@code true}
   */
  boolean builder() default true;

  /**
   * <p>
   * Default null checking behaviour for the non-primitive properties of this goal.
   * </p><p>
   * Can be overridden in {@link Step}.
   * </p>
   *
   * @return null policy setting
   */
  NullPolicy nullPolicy() default ALLOW;

  /**
   * <p>A handle to override the default access level of the generated static builder method.</p>
   * <p>If {@link #builder} {@code == false}, then this setting has no effect.</p>
   *
   * @return builder access level
   * @see Builders#access()
   */
  AccessLevel builderAccess() default UNSPECIFIED;

  /**
   * <p>A handle to override the default access level of the generated static toBuilder method.</p>
   * <p>If {@link #toBuilder} is not set to {@code true}, then this setting has no effect.</p>
   *
   * @return toBuilder access level
   * @see Builders#access()
   */
  AccessLevel toBuilderAccess() default UNSPECIFIED;
}
