package net.zerobuilder.compiler.generate;

import com.google.common.base.Joiner;
import com.squareup.javapoet.ClassName;
import net.zerobuilder.Builders;

import javax.lang.model.element.TypeElement;

public final class BuilderType {

  final boolean recycle;

  /**
   * The type that carries the {@link Builders} annotation
   */
  public final ClassName annotatedType;

  /**
   * The type that will be generated: {@code annotatedType + "Builders"}
   */
  public final ClassName generatedType;

  private BuilderType(boolean recycle, ClassName annotatedType, ClassName generatedType) {
    this.recycle = recycle;
    this.annotatedType = annotatedType;
    this.generatedType = generatedType;
  }

  public static BuilderType createBuilderContext(TypeElement buildElement) {
    boolean nogc = buildElement.getAnnotation(Builders.class).recycle();
    ClassName generatedType = generatedClassName(buildElement);
    ClassName annotatedType = ClassName.get(buildElement);
    return new BuilderType(nogc, annotatedType, generatedType);
  }

  private static ClassName generatedClassName(TypeElement buildElement) {
    ClassName sourceType = ClassName.get(buildElement);
    String simpleName = Joiner.on('_').join(sourceType.simpleNames()) + "Builders";
    return sourceType.topLevelClassName().peerClass(simpleName);
  }
}
