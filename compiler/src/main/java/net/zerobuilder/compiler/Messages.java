package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;

import javax.annotation.Generated;
import javax.lang.model.util.Elements;

final class Messages {

  static final class ErrorMessages {

    static final String PRIVATE_METHOD =
        "The goal may not be private.";

    static final String PRIVATE_TYPE =
        "The @Builder annotated type may not be private.";

    static final String NOT_ENOUGH_PARAMETERS =
        "The goal must have at least one parameter.";

    static final String NESTING_KIND =
        "The @Builder annotation can only be used on top level and non-private static inner classes.";

    static final String NO_GOALS =
        "No goals were found.";

    static final String MULTIPLE_TOBUILDER =
        "Only one goal can have the toBuilder flag set.";

    static final String GOAL_NOT_IN_BUILD =
        "The @Goal annotation may not appear outside a class that carries the @Builder annotation.";

    /* empty, empty, constructor, constructor */
    static final String GOALNAME_EECC =
        "Multiple constructor goals found. Please add a goal name.";

    static final String NEGATIVE_STEP_POSITION =
        "Negative step position is not allowed.";

    static final String STEP_POSITION_TOO_LARGE =
        "Step position must be less than the number of arguments.";

    static final String DUPLICATE_STEP_POSITION =
        "Step position is specified twice.";

    /* empty, empty, method, constructor */
    static final String GOALNAME_EEMC =
        "There is already a constructor goal for this return type. Please add a goal name.";

    /* empty, empty, method, method */
    static final String GOALNAME_EEMM =
        "There is already another goal for this return type. Please add a goal name.";

    /* named, empty, constructor, constructor */
    static final String GOALNAME_NECC =
        "This goal name is taken by another constructor.";

    /* named, empty, method, constructor */
    static final String GOALNAME_NEMC =
        "This goal name is taken by a constructor.";

    /* named, empty, method, method */
    static final String GOALNAME_NEMM =
        "This goal name is taken by another goal.";

    /* named, named */
    static final String GOALNAME_NN =
        "There is another goal name with this goal name.";

    private ErrorMessages() {
    }

  }

  static final class JavadocMessages {

    static final String GENERATED_COMMENTS = "https://github.com/h908714124/zerobuilder";

    static ImmutableList<AnnotationSpec> generatedAnnotations(Elements elements) {
      if (elements.getTypeElement("javax.annotation.Generated") != null) {
        return ImmutableList.of(AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", ZeroProcessor.class.getName())
            .addMember("comments", "$S", GENERATED_COMMENTS)
            .build());
      }
      return ImmutableList.of();

    }

    private JavadocMessages() {
    }

  }

  private Messages() {
  }

}
