package isobuilder.compiler;

final class ErrorMessages {

  static final String NON_STATIC_METHOD =
      "Target method must be static.";

  static final String PRIVATE_METHOD =
      "Target method may not be private.";

  private ErrorMessages() {}

}
