package net.zerobuilder.compiler;

import com.squareup.javapoet.ClassName;

abstract class GenerationContext {
  abstract ClassName builderType();
}
