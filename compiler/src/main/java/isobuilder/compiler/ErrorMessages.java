package isobuilder.compiler;

final class ErrorMessages {

  static final String NON_STATIC_METHOD =
      "The Builder annotation can only be used on static methods or constructors.";

  static final String PRIVATE_METHOD =
      "The annotated element may not be private.";

  static final String PRIVATE_CLASS =
      "The class that contains the annotated element may not be private.";

  static final String DUPLICATE =
      "Only one element per class may have the Builder annotation.";

  static final String TOO_FEW_PARAMETERS =
      "The annotated element may not have less than two parameters.";

  static final String NESTING_KIND =
      "The Builder annotation can only be used on top level classes and static inner classes.";

  private ErrorMessages() {
  }

}
