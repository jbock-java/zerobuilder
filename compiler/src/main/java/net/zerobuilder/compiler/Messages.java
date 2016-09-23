package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;

import javax.annotation.Generated;
import javax.lang.model.util.Elements;

public final class Messages {

  public static final class ErrorMessages {

    private static final String POJO_HINT
        = " If this is not a POJO, try putting the @Goal annotation on a constructor instead.";

    private static final String GOAL_NAME_HINT
        = " Goal name conflicts can be resolved with the @Goal(name = ...) attribute.";

    public static final String PRIVATE_METHOD =
        "The goal may not be private.";

    public static final String PRIVATE_TYPE =
        "The @Builders annotated class may not be private.";

    public static final String NOT_ENOUGH_PARAMETERS =
        "The goal must have at least one parameter.";

    public static final String NESTING_KIND =
        "The @Builders annotation can only be used on top level and non-private static inner classes.";

    public static final String NO_GOALS =
        "No goals were found.";

    public static final String GOAL_NOT_IN_BUILD =
        "The @Goal annotation may not appear outside a class that carries the @Builders annotation.";

    public static final String GOAL_WITHOUT_BUILDERS =
        "A class that carries the @Goal annotation must also carry the @Builders annotation." + POJO_HINT;

    public static final String STEP_POSITION_TOO_LARGE =
        "Step position must be less than the number of arguments.";

    public static final String DUPLICATE_STEP_POSITION =
        "Step position is specified twice.";

    /* empty, empty, constructor, constructor */
    public static final String GOALNAME_EECC =
        "Multiple constructor goals found. Please add a goal name.";

    /* empty, empty, method, constructor */
    public static final String GOALNAME_EEMC =
        "There is already a constructor goal for this return type." + GOAL_NAME_HINT;

    /* empty, empty, method, method */
    public static final String GOALNAME_EEMM =
        "There is already another goal for this return type." + GOAL_NAME_HINT;

    /* named, empty, constructor, constructor */
    public static final String GOALNAME_NECC =
        "This goal name is taken by another constructor." + GOAL_NAME_HINT;

    /* named, empty, method, constructor */
    public static final String GOALNAME_NEMC =
        "This goal name is taken by a constructor." + GOAL_NAME_HINT;

    /* named, empty, method, method */
    public static final String GOALNAME_NEMM =
        "This goal name is taken by another goal." + GOAL_NAME_HINT;

    /* named, named */
    public static final String GOALNAME_NN =
        "There is another goal with this goal name." + GOAL_NAME_HINT;

    public static final String NO_DEFAULT_CONSTRUCTOR
        = "Class not public or no public default constructor found." + POJO_HINT;

    public static final String TARGET_PUBLIC
        = "Target type must be public." + POJO_HINT;

    public static final String GETTER_EXCEPTION
        = "POJO getters may not declare exceptions." + POJO_HINT;

    public static final String GETTER_SETTER_TYPE_MISMATCH
        = "Getter/setter type mismatch." + POJO_HINT;

    public static final String BAD_GENERICS
        = "Can't understand the generics of this accessor pair." + POJO_HINT;

    public static final String COULD_NOT_FIND_SETTER
        = "Could not find setter." + POJO_HINT;

    public static final String SETTER_EXCEPTION
        = "Setters may not declare exceptions." + POJO_HINT;

    public static final String NO_PROJECTION
        = "Could not find a projection (getter or field).";

    public static final String IGNORE_AND_STEP =
        "@Ignore and @Step don't make sense together." + POJO_HINT;

    public static final String STEP_ON_SETTER
        = "The @Step annotation goes on getters, not setters.";

    public static final String IGNORE_ON_SETTER
        = "The @Ignore annotation goes on getters, not setters.";

    public static final String NO_ACCESSOR_PAIRS
        = "No accessor pairs found." + POJO_HINT;

    private ErrorMessages() {
      throw new UnsupportedOperationException("no instances");
    }
  }

  public static final class JavadocMessages {

    public static final String GENERATED_COMMENTS = "https://github.com/h908714124/zerobuilder";

    public static ImmutableList<AnnotationSpec> generatedAnnotations(Elements elements) {
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
