package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static net.zerobuilder.compiler.test_util.GeneratedLines.GENERATED_ANNOTATION;

public class StepOrderTest {

  @Test
  public void instance() {
    JavaFileObject cube = forSourceLines("cube.Spaghetti",
        "package cube;",
        "import net.zerobuilder.*;",
        "",
        "final class Spaghetti {",
        "  final String cheese;",
        "  final String sauce;",
        "  @Builder",
        "  Spaghetti(String cheese, @Step(0) String sauce) {",
        "    this.cheese = cheese;",
        "    this.sauce = sauce;",
        "  }",
        "}");
    JavaFileObject expected =
        forSourceLines("cube.SpaghettiBuilders",
            "package cube;",
            "import javax.annotation.Generated;",
            "",
            GENERATED_ANNOTATION,
            "public final class SpaghettiBuilders {",
            "  private SpaghettiBuilders() {",
            "    throw new UnsupportedOperationException(\"no instances\");",
            "  }",
            "",
            "  public static SpaghettiBuilder.Sauce spaghettiBuilder() {",
            "    return new SpaghettiBuilderImpl();",
            "  }",
            "",
            "  private static final class SpaghettiBuilderImpl implements SpaghettiBuilder.Sauce, SpaghettiBuilder.Cheese {",
            "    private String sauce;",
            "",
            "    SpaghettiBuilderImpl() {}",
            "",
            "    @Override",
            "    public SpaghettiBuilder.Cheese sauce(String sauce) {",
            "      this.sauce = sauce;",
            "      return this;",
            "    }",
            "",
            "    @Override",
            "    public Spaghetti cheese(String cheese) {",
            "      Spaghetti _spaghetti = new Spaghetti(sauce, cheese);",
            "      return _spaghetti;",
            "    }",
            "  }",
            "",
            "  public static final class SpaghettiBuilder {",
            "    private SpaghettiBuilder() {",
            "      throw new UnsupportedOperationException(\"no instances\");",
            "    }",
            "    public interface Sauce {",
            "      Cheese sauce(String sauce);",
            "    }",
            "    public interface Cheese {",
            "      Spaghetti cheese(String cheese);",
            "    }",
            "  }",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(cube))
        .processedWith(new ZeroProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }
}
