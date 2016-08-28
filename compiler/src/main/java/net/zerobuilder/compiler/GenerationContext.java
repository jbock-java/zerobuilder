package net.zerobuilder.compiler;

import com.squareup.javapoet.ClassName;

interface GenerationContext {
  ClassName generatedTypeName();
}
