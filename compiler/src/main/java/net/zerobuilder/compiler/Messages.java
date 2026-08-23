package net.zerobuilder.compiler;

import com.palantir.javapoet.AnnotationSpec;
import java.util.List;
import javax.annotation.processing.Generated;

public final class Messages {

  public static final class ErrorMessages {

    public static final String PRIVATE_METHOD =
        "The goal method may not be private.";

    public static final String ABSTRACT_CONSTRUCTOR =
        "An abstract class may not have constructor goals." +
            " Try using a static factory method instead.";

    public static final String NESTING_KIND =
        "This inner class must be static and not private.";

    public static final String STEP_OUT_OF_BOUNDS =
        "The step position must be less than the number of arguments.";

    public static final String STEP_DUPLICATE =
        "The step position is specified twice.";

    public static final String DUPLICATE_GOAL_NAME =
        "There is another goal with this name. " +
            "This naming conflict can be resolved by using the @GoalName annotation.";

    public static final String MISSING_PROJECTION =
        "Missing projection: ";

    private ErrorMessages() {
      throw new UnsupportedOperationException("no instances");
    }
  }

  public static final class JavadocMessages {

    public static final String GENERATED_COMMENTS = "https://github.com/jbock-java/zerobuilder";

    static List<AnnotationSpec> generatedAnnotations() {
      return List.of(AnnotationSpec.builder(Generated.class)
          .addMember("value", "$S", ZeroProcessor.class.getName())
          .addMember("comments", "$S", GENERATED_COMMENTS)
          .build());
    }

    private JavadocMessages() {
      throw new UnsupportedOperationException("no instances");
    }
  }

  private Messages() {
    throw new UnsupportedOperationException("no instances");
  }
}
