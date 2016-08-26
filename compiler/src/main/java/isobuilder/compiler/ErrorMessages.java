package isobuilder.compiler;

final class ErrorMessages {

  static final String NON_STATIC_METHOD =
      "The Builder annotation can only be used on static methods or constructors.";

  static final String PRIVATE_METHOD =
      "The annotated method may not be private.";

  static final String DUPLICATE =
      "Only one method per class may have the Builder annotation.";

  private ErrorMessages() {
  }

}
