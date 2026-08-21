package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static com.palantir.javapoet.TypeSpec.anonymousClassBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.ClassNames.THREAD_LOCAL;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;

public final class DtoContext {

  public static final class GoalContext {

    /**
     * The type that should be generated.
     */
    public final ClassName generatedType;

    /**
     * The class that contains the goal method(s) or constructor(s).
     * This is either a {@link ClassName} or a {@link ParameterizedTypeName}.
     */
    public final TypeName type;

    private GoalContext(TypeName type, ClassName generatedType) {
      this.type = type;
      this.generatedType = generatedType;
    }

    public FieldSpec cache(String className) {
      ClassName cachedClass = generatedType.nestedClass(className);
      ParameterizedTypeName type = ParameterizedTypeName.get(THREAD_LOCAL,
          cachedClass);
      TypeSpec initializer = anonymousClassBuilder("")
          .addSuperinterface(type)
          .addMethod(methodBuilder("initialValue")
              .addAnnotation(Override.class)
              .addModifiers(PROTECTED)
              .returns(cachedClass)
              .addStatement("return new $T()", cachedClass)
              .build())
          .build();
      return FieldSpec.builder(type, downcase(className))
          .initializer("$L", initializer)
          .addModifiers(PRIVATE, STATIC, FINAL)
          .build();
    }

    public FieldSpec cache(ClassName className) {
      return cache(className.simpleName());
    }
  }

  /**
   * Create metadata for goal processing.
   *
   * @param type          type that contains the goal methods / constructors;
   *                      for bean goals, this is just the bean type
   * @param generatedType type name that should be generated
   * @return a GoalContext
   */
  public static GoalContext createContext(TypeName type,
                                          ClassName generatedType) {
    return new GoalContext(type, generatedType);
  }

  private DtoContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
