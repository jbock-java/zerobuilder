package net.zerobuilder.compiler;

import com.google.common.base.Joiner;
import com.squareup.javapoet.ClassName;
import net.zerobuilder.Builders;

import javax.lang.model.element.TypeElement;

import static javax.lang.model.element.Modifier.PUBLIC;

final class BuilderContext {

  final boolean recycle;

  /**
   * The type that carries the {@link Builders} annotation
   */
  final ClassName annotatedType;

  /**
   * The type that will be generated: {@code annotatedType + "Builders"}
   */
  final ClassName generatedType;

  private BuilderContext(boolean recycle, ClassName annotatedType, ClassName generatedType) {
    this.recycle = recycle;
    this.annotatedType = annotatedType;
    this.generatedType = generatedType;
  }

  static BuilderContext createBuildConfig(TypeElement buildElement) {
    boolean nogc = buildElement.getAnnotation(Builders.class).recycle();
    ClassName generatedType = generatedClassName(buildElement);
    ClassName annotatedType = ClassName.get(buildElement);
    return new BuilderContext(nogc, annotatedType, generatedType);
  }

  private static ClassName generatedClassName(TypeElement buildElement) {
    ClassName sourceType = ClassName.get(buildElement);
    String simpleName = Joiner.on('_').join(sourceType.simpleNames()) + "Builders";
    return sourceType.topLevelClassName().peerClass(simpleName);
  }
}
