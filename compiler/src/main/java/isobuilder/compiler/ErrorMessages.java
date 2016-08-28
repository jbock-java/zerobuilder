package isobuilder.compiler;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

final class ErrorMessages {

  static final String NON_STATIC_METHOD =
      "The Build annotation can only be used on static methods or constructors.";

  static final String PRIVATE_METHOD =
      "The annotated element may not be private.";

  static final String PRIVATE_CLASS =
      "The class that contains the annotated element may not be private.";

  static final String NOT_ENOUGH_PARAMETERS =
      "The annotated element may not have less than two parameters.";

  static final String NESTING_KIND =
      "The Build annotation can only be used on top level classes and static inner classes.";

  static final String METHOD_NOT_FOUND =
      "There should be a constructor or static method that carries the @Builder.From annotation.";

  static final String SEVERAL_METHODS =
      "The @Builder.From annotation may not appear more than once per class.";

  static final String RETURN_TYPE =
      "Static factory methods must return the type of the enclosing class.";

  static final String MATCH_ERROR =
      "Could not correlate accessors or fields with parameters.";

  static final String INFO_BUILDER_JAVADOC = Joiner.on('\n').join(ImmutableList.of(
      "The first step of the builder chain that builds {@link $T}.",
      "All steps of the builder implementation are mutable.",
      "It is not recommended to use any of the steps more than once.",
      "@return A mutable builder without any thread safety guarantees.", ""));


  private ErrorMessages() {
  }

}
