package net.zerobuilder.compiler.generate;

import com.google.common.base.Joiner;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import net.zerobuilder.Builders;

import javax.lang.model.element.TypeElement;

import static javax.lang.model.element.Modifier.PRIVATE;
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

    private BuildersContext(boolean recycle, ClassName type, ClassName generatedType, FieldSpec field) {
      this.recycle = recycle;
      this.type = type;
      this.generatedType = generatedType;
      this.field = field;
    }
  }


  public static BuildersContext createBuildersContext(TypeElement buildElement) {
    boolean recycle = buildElement.getAnnotation(Builders.class).recycle();
    ClassName generatedType = generatedClassName(buildElement);
    ClassName annotatedType = ClassName.get(buildElement);
    FieldSpec field = FieldSpec.builder(
        annotatedType, '_' + downcase(annotatedType.simpleName()), PRIVATE).build();
    return new BuildersContext(recycle, annotatedType, generatedType, field);
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
