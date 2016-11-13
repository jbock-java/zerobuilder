package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.function.Supplier;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.anonymousClassBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.ClassNames.THREAD_LOCAL;
import static net.zerobuilder.compiler.generate.ZeroUtil.memoize;

public final class DtoContext {

  public enum ContextLifecycle {
    REUSE_INSTANCES, NEW_INSTANCE;
  }

  public static final class GoalContext {

    /**
     * The type that should be generated.
     */
    public final ClassName generatedType;

    public final ContextLifecycle lifecycle;

    /**
     * The class that contains the goal method(s) or constructor(s).
     * This is either a {@link ClassName} or a {@link ParameterizedTypeName}.
     */
    public final TypeName type;

    /**
     * An instance of {@code ThreadLocal} that holds an instance of {@link #generatedType}.
     * Only used when {@link #lifecycle} is
     * {@link ContextLifecycle#REUSE_INSTANCES REUSE_INSTANCES}.
     */
    public final Supplier<FieldSpec> cache;

    private GoalContext(ContextLifecycle lifecycle, ClassName type, ClassName generatedType) {
      this.lifecycle = lifecycle;
      this.type = type;
      this.generatedType = generatedType;
      this.cache = memoizeCache(generatedType);
    }
  }

  /**
   * Create metadata for goal processing.
   *
   * @param type             type that contains the goal methods / constructors;
   *                         for bean goals, this is just the bean type
   * @param generatedType    type name that should be generated
   * @param contextLifecycle lifecycle setting
   * @return a GoalContext
   */
  public static GoalContext createContext(ClassName type,
                                          ClassName generatedType,
                                          ContextLifecycle contextLifecycle) {
    return new GoalContext(contextLifecycle, type, generatedType);
  }

  private static Supplier<FieldSpec> memoizeCache(ClassName generatedType) {
    ParameterizedTypeName type = ParameterizedTypeName.get(THREAD_LOCAL, generatedType);
    TypeSpec initializer = anonymousClassBuilder("")
        .addSuperinterface(type)
        .addMethod(methodBuilder("initialValue")
            .addAnnotation(Override.class)
            .addModifiers(PROTECTED)
            .returns(generatedType)
            .addStatement("return new $T()", generatedType)
            .build())
        .build();
    return memoize(() -> FieldSpec.builder(type, "INSTANCE")
        .initializer("$L", initializer)
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build());
  }

  private DtoContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
