package net.zerobuilder.compiler;

import io.jbock.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static io.jbock.testing.compile.CompilationSubject.assertThat;
import static io.jbock.testing.compile.JavaFileObjects.forSourceLines;
import static net.zerobuilder.compiler.Compilers.simpleCompiler;

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
    Compilation compilation = simpleCompiler().compile(cube);
    assertThat(compilation).succeeded();
    assertThat(compilation).generatedSourceFile("cube.SpaghettiBuilders")
        .containsLines(
            "package cube;",
            "import javax.annotation.processing.Generated;",
            "",
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
            "    SpaghettiBuilderImpl() {",
            "    }",
            "",
            "    @Override",
            "    public SpaghettiBuilder.Cheese sauce(String sauce) {",
            "      this.sauce = sauce;",
            "      return this;",
            "    }",
            "",
            "    @Override",
            "    public Spaghetti cheese(String cheese) {",
            "      Spaghetti _spaghetti = new Spaghetti(cheese, sauce);",
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
  }
}
