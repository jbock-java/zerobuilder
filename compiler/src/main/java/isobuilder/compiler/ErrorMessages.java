package isobuilder.compiler;

final class ErrorMessages {

  static final String NON_STATIC_METHOD =
      "The Build annotation can only be used on static methods or constructors.";

  static final String PRIVATE_METHOD =
      "The annotated element may not be private.";

  static final String PRIVATE_CLASS =
      "The class that contains the annotated element may not be private.";

  static final String TOO_FEW_PARAMETERS =
      "The annotated element may not have less than two parameters.";

  static final String NESTING_KIND =
      "The Build annotation can only be used on top level classes and static inner classes.";

  static final String METHOD_NOT_FOUND =
      "There should be a constructor or static method that carries the @Builder.From annotation.";

  static final String SEVERAL_METHODS =
      "The @Builder.From annotation may not appear more than once per class.";

  static final String RETURN_TYPE =
      "Static factory methods must return the type of the enclosing class.";

  private ErrorMessages() {
  }

}
