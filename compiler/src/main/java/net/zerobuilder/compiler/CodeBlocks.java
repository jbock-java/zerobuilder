package net.zerobuilder.compiler;

import com.squareup.javapoet.CodeBlock;

import java.util.Iterator;

final class CodeBlocks {

  /**
   * Returns a comma-separated version of {@code codeBlocks} as one unified {@link CodeBlock}.
   */
  static CodeBlock makeParametersCodeBlock(Iterable<CodeBlock> codeBlocks) {
    return join(codeBlocks, ", ");
  }

  static CodeBlock.Builder join(
      CodeBlock.Builder builder, Iterable<CodeBlock> codeBlocks, String delimiter) {
    Iterator<CodeBlock> iterator = codeBlocks.iterator();
    while (iterator.hasNext()) {
      builder.add(iterator.next());
      if (iterator.hasNext()) {
        builder.add(delimiter);
      }
    }
    return builder;
  }

  static CodeBlock join(Iterable<CodeBlock> codeBlocks, String delimiter) {
    return join(CodeBlock.builder(), codeBlocks, delimiter).build();
  }

  private CodeBlocks() {
  }
}
