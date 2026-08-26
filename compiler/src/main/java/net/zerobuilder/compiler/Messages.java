package net.zerobuilder.compiler;

public final class Messages {

  public static final class ErrorMessages {

    public static final String PRIVATE_METHOD =
        "The goal method may not be private.";

    public static final String NESTING_KIND =
        "This inner class must be static and not private.";

    public static final String STEP_OUT_OF_BOUNDS =
        "The step position must be less than the number of arguments.";

    public static final String STEP_DUPLICATE =
        "The step position is specified twice.";

    public static final String MISSING_PROJECTION =
        "Missing projection: ";

    private ErrorMessages() {
    }
  }

  private Messages() {
  }
}
