package net.zerobuilder.compiler;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;

import javax.annotation.Generated;
import javax.lang.model.util.Elements;

final class Messages {

  static final class ErrorMessages {
    static final String NON_STATIC_METHOD =
        "The @Build annotation can only be used on static methods or constructors.";

    static final String PRIVATE_METHOD =
        "The @Build.Via annotated method may not be private.";

    static final String NOT_ENOUGH_PARAMETERS =
        "The @Build.Via annotated method has no parameters. Skipping code generation.";

    static final String NESTING_KIND =
        "The @Build annotation can only be used on top level and non-private static inner classes.";

    static final String METHOD_NOT_FOUND =
        "The class should have a constructor or static method that carries the @Build.Via annotation.";

    static final String SEVERAL_METHODS =
        "The @Build.Via annotation may not appear more than once per class.";

    static final String RETURN_TYPE =
        "Static factory methods must return the type of the @Build annotated class, " +
            "or the type specified in @Build(goal = ...).";

    static final String MATCH_ERROR =
        "Could not correlate accessors with parameters. Consider using @Build(toBuilder = false).";

    static final String GOAL_VIA_CONSTRUCTOR =
        "Cannot build goal type via constructor. Consider using a static metho.";

    private ErrorMessages() {
    }

  }

  static final class JavadocMessages {

    static final String JAVADOC_BUILDER = Joiner.on('\n').join(ImmutableList.of(
        "First step of the builder chain that builds {@link $T}.",
        "@return A builder object", ""));

    static final String GENERATED_COMMENTS = "https://github.com/h908714124/zerobuilder";

    static ImmutableList<AnnotationSpec> generatedAnnotations(Elements elements) {
      if (elements.getTypeElement("javax.annotation.Generated") != null) {
        return ImmutableList.of(AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", MyProcessor.class.getName())
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
