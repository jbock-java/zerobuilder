package net.zerobuilder.compiler;

import com.squareup.javapoet.AnnotationSpec;

import javax.annotation.Generated;
import javax.lang.model.util.Elements;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;

public final class Messages {

  public static final class ErrorMessages {

    public static final String PRIVATE_METHOD =
        "The goal method may not be private.";

    public static final String ABSTRACT_CONSTRUCTOR =
        "An abstract class may not have constructor goals." +
            " Try using a static factory method instead.";

    public static final String PRIVATE_TYPE =
        "The class may not be private.";

    public static final String NESTING_KIND =
        "The class must be top level or non-private static inner.";

    public static final String STEP_OUT_OF_BOUNDS =
        "Step position must be less than the number of arguments.";

    public static final String STEP_DUPLICATE =
        "Step position is specified twice.";

    public static final String DUPLICATE_GOAL_NAME =
        "There is another goal with this name. " +
            "Try using the @GoalName annotation.";

    public static final String BEAN_NO_DEFAULT_CONSTRUCTOR =
        "A non-private default constructor must exist.";

    public static final String BEAN_PRIVATE_CLASS =
        "This class may not be private.";

    public static final String BEAN_ABSTRACT_CLASS =
        "This class may not be abstract.";

    public static final String BEAN_COULD_NOT_FIND_SETTER =
        "Could not find the setter.";

    public static final String MISSING_PROJECTION =
        "Problem while generating an updater: Missing projection.";

    public static final String IGNORE_ON_SETTER =
        "The @IgnoreGetter annotation goes on getters, not setters.";

    public static final String BEAN_SUBGOALS =
        "Bean goals may not have subgoals.";

    public static final String TYPE_PARAMS_BEAN =
        "Type parameters are not allowed in bean goals.";

    private ErrorMessages() {
      throw new UnsupportedOperationException("no instances");
    }
  }

  public static final class JavadocMessages {

    public static final String GENERATED_COMMENTS = "https://github.com/h908714124/zerobuilder";

    static List<AnnotationSpec> generatedAnnotations(Elements elements) {
      if (elements.getTypeElement("javax.annotation.Generated") != null) {
        return singletonList(AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", ZeroProcessor.class.getName())
            .addMember("comments", "$S", GENERATED_COMMENTS)
            .build());
      }
      return Collections.emptyList();

    }

    private JavadocMessages() {
      throw new UnsupportedOperationException("no instances");
    }
  }

  private Messages() {
    throw new UnsupportedOperationException("no instances");
  }
}
