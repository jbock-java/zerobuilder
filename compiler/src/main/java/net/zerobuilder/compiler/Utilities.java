package net.zerobuilder.compiler;

import com.squareup.javapoet.CodeBlock;

import java.util.Iterator;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;

final class Utilities {

  static String upcase(String s) {
    return LOWER_CAMEL.to(UPPER_CAMEL, s);
  }

  static String downcase(String s) {
    return UPPER_CAMEL.to(LOWER_CAMEL, s);
  }

  static CodeBlock joinCodeBlocks(Iterable<CodeBlock> codeBlocks, String delimiter) {
    CodeBlock.Builder builder = CodeBlock.builder();
    Iterator<CodeBlock> iterator = codeBlocks.iterator();
    while (iterator.hasNext()) {
      builder.add(iterator.next());
      if (iterator.hasNext()) {
        builder.add(delimiter);
      }
    }
    return builder.build();
  }

  private Utilities() {
  }

}
