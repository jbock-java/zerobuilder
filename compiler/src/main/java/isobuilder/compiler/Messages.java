package isobuilder.compiler;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;

import javax.annotation.Generated;
import javax.lang.model.util.Elements;

final class Messages {

  static final class ErrorMessages {
    static final String NON_STATIC_METHOD =
        "The Build annotation can only be used on static methods or constructors.";

    static final String PRIVATE_METHOD =
        "The annotated element may not be private.";

    static final String PRIVATE_CLASS =
        "The class that contains the annotated element may not be private.";

    static final String NOT_ENOUGH_PARAMETERS =
        "The annotated element may not have less than two parameters.";

    static final String NESTING_KIND =
        "The Build annotation can only be used on top level classes and static inner classes.";

    static final String METHOD_NOT_FOUND =
        "There should be a constructor or static method that carries the @Build.Via annotation.";

    static final String SEVERAL_METHODS =
        "The @Build.Via annotation may not appear more than once per class.";

    static final String RETURN_TYPE =
        "Static factory methods must return the type of the enclosing class.";

    static final String MATCH_ERROR =
        "Could not correlate accessors or fields with parameters.";

    private ErrorMessages() {
    }

  }

  static final class JavadocMessages {

    static final String JAVADOC_BUILDER = Joiner.on('\n').join(ImmutableList.of(
        "The first step of the builder chain that builds {@link $T}.",
        "All steps of the builder implementation are mutable.",
        "It is not recommended to use any of the steps more than once.",
        "@return A mutable builder without any thread safety guarantees.", ""));

    static final String GENERATED_COMMENTS = "https://github.com/h908714124/isobuilder";

    static ImmutableList<AnnotationSpec> generatedAnnotations(Elements elements) {
      if (elements.getTypeElement("javax.annotation.Generated") != null) {
        return ImmutableList.of(AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", IsoProcessor.class.getName())
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
