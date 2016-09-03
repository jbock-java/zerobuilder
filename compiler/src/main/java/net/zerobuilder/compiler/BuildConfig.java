package net.zerobuilder.compiler;

import com.google.common.base.Joiner;
import com.squareup.javapoet.ClassName;
import net.zerobuilder.Build;

import javax.lang.model.element.TypeElement;

import static javax.lang.model.element.Modifier.PUBLIC;

final class BuildConfig {

  final boolean nogc;
  final boolean toBuilder;
  final boolean isPublic;
  final ClassName annotatedType;
  final ClassName generatedType;

  private BuildConfig(boolean nogc, boolean toBuilder, boolean isPublic, ClassName annotatedType, ClassName generatedType) {
    this.nogc = nogc;
    this.toBuilder = toBuilder;
    this.isPublic = isPublic;
    this.annotatedType = annotatedType;
    this.generatedType = generatedType;
  }

  static BuildConfig createBuildConfig(TypeElement buildElement) {
    boolean nogc = buildElement.getAnnotation(Build.class).nogc();
    boolean toBuilder = buildElement.getAnnotation(Build.class).toBuilder();
    boolean isPublic = buildElement.getModifiers().contains(PUBLIC);
    ClassName generatedType = generatedClassName(buildElement);
    ClassName annotatedType = ClassName.get(buildElement);
    return new BuildConfig(nogc, toBuilder, isPublic, annotatedType, generatedType);
  }

  private static ClassName generatedClassName(TypeElement buildElement) {
    ClassName sourceType = ClassName.get(buildElement);
    String simpleName = Joiner.on('_').join(sourceType.simpleNames()) + "Builders";
    return sourceType.topLevelClassName().peerClass(simpleName);
  }

}
