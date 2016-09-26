package net.zerobuilder.compiler.generate;

import com.google.common.base.Joiner;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import net.zerobuilder.Builders;

import javax.lang.model.element.TypeElement;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.anonymousClassBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Utilities.downcase;

public final class DtoBuilders {

  public static final class BuildersContext {
    final boolean recycle;

    /**
     * The class that carries the {@link Builders} annotation
     */
    public final ClassName type;

    /**
     * The type that will be generated: {@link #type} {@code + "Builders"}
     */
    public final ClassName generatedType;

    /**
     * Only used when kind is {@link net.zerobuilder.compiler.analyse.GoalContextFactory.GoalKind#INSTANCE_METHOD}
     */
    public final FieldSpec field;

    /**
     * An optional {@code ThreadLocal} that holds an instance of the generated type, if {@link #recycle}.
     */
    public final FieldSpec cache;

    private BuildersContext(boolean recycle, ClassName type, ClassName generatedType, FieldSpec field, FieldSpec tl) {
      this.recycle = recycle;
      this.type = type;
      this.generatedType = generatedType;
      this.field = field;
      this.cache = tl;
    }
  }


  public static BuildersContext createBuildersContext(TypeElement buildElement) {
    boolean recycle = buildElement.getAnnotation(Builders.class).recycle();
    ClassName generatedType = generatedClassName(buildElement);
    ClassName annotatedType = ClassName.get(buildElement);
    FieldSpec field = FieldSpec.builder(
        annotatedType, '_' + downcase(annotatedType.simpleName()), PRIVATE).build();
    FieldSpec cache = defineCache(generatedType);
    return new BuildersContext(recycle, annotatedType, generatedType, field, cache);
  }

  private static FieldSpec defineCache(ClassName generatedType) {
    return FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(ThreadLocal.class),
          generatedType), "INSTANCE")
          .initializer("$L", anonymousClassBuilder("")
              .addSuperinterface(ParameterizedTypeName.get(ClassName.get(ThreadLocal.class),
                  generatedType))
              .addMethod(methodBuilder("initialValue")
                  .addAnnotation(Override.class)
                  .addModifiers(PROTECTED)
                  .returns(generatedType)
                  .addStatement("return new $T()", generatedType)
                  .build())
              .build())
          .addModifiers(PRIVATE, STATIC, FINAL)
          .build();
  }

  private static ClassName generatedClassName(TypeElement buildElement) {
    ClassName sourceType = ClassName.get(buildElement);
    String simpleName = Joiner.on('_').join(sourceType.simpleNames()) + "Builders";
    return sourceType.topLevelClassName().peerClass(simpleName);
  }

  private DtoBuilders() {
    throw new UnsupportedOperationException("no instances");
  }
}
