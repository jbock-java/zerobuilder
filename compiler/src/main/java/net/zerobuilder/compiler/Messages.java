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
        "The @Builders annotated type may not be private.";

    static final String NOT_ENOUGH_PARAMETERS =
        "The goal must have at least one parameter.";

    static final String NESTING_KIND =
        "The @Builders annotation can only be used on top level and non-private static inner classes.";

    static final String NO_GOALS =
        "No goals were found.";

    static final String GOAL_NOT_IN_BUILD =
        "The @Goal annotation may not appear outside a class that carries the @Builders annotation.";

    static final String GOAL_WITHOUT_BUILDERS =
        "A class that carries the @Goal annotation must also carry the @Builders annotation.";

    static final String NEGATIVE_STEP_POSITION =
        "Negative step position is not allowed.";

    static final String STEP_POSITION_TOO_LARGE =
        "Step position must be less than the number of arguments.";

    static final String DUPLICATE_STEP_POSITION =
        "Step position is specified twice.";

    /* empty, empty, constructor, constructor */
    static final String GOALNAME_EECC =
        "Multiple constructor goals found. Please add a goal name.";

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
        "There is another goal with this goal name.";

    static final String NO_DEFAULT_CONSTRUCTOR
        = "Class not public or no public default constructor found: ";

    static final String TARGET_PUBLIC
        = "Target type must be public";

    static final String GETTER_EXCEPTION
        = "Getter may not declare exception";

    static final String GETTER_SETTER_TYPE_MISMATCH
        = "Getter/setter type mismatch";

    static final String BAD_GENERICS
        = "Can't understand the generics of this beanGoal";

    static final String COULD_NOT_FIND_SETTER
        = "Could not find setter";

    static final String NO_PROJECTION
        = "Could not find projection";

    static final String SETTER_EXCEPTION
        = "Setter may not declare exception";

    static final String NOT_A_BEAN
        = "This type cannot be a bean";

    private ErrorMessages() {
      throw new UnsupportedOperationException("no instances");
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
      throw new UnsupportedOperationException("no instances");
    }
  }

  private Messages() {
    throw new UnsupportedOperationException("no instances");
  }
}
