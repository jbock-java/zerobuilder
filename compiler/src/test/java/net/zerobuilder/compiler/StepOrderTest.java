package net.zerobuilder.compiler;

import io.jbock.testing.compile.Compilation;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

import static io.jbock.testing.compile.CompilationSubject.assertThat;
import static io.jbock.testing.compile.JavaFileObjects.forSourceLines;
import static net.zerobuilder.compiler.Compilers.simpleCompiler;

class StepOrderTest {

  @Test
  void instance() {
    JavaFileObject cube = forSourceLines("cube.Spaghetti",
        "package cube;",
        "import net.zerobuilder.Builder;",
        "import net.zerobuilder.Step;",
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
            "  public static SauceStep spaghettiBuilder() {",
            "    return new SpaghettiBuilderImpl();",
            "  }",
            "",
            "  private static final class SpaghettiBuilderImpl implements SauceStep, CheeseStep {",
            "    private String sauce;",
            "",
            "    SpaghettiBuilderImpl() {",
            "    }",
            "",
            "    @Override",
            "    public CheeseStep sauce(String sauce) {",
            "      this.sauce = sauce;",
            "      return this;",
            "    }",
            "",
            "    @Override",
            "    public Spaghetti cheese(String cheese) {",
            "      return new Spaghetti(cheese, sauce);",
            "    }",
            "  }",
            "",
            "  public static final class SpaghettiBuilder {",
            "    private SpaghettiBuilder() {",
            "      throw new UnsupportedOperationException(\"no instances\");",
            "    }",
            "  }",
            "",
            "  public interface SauceStep {",
            "    CheeseStep sauce(String sauce);",
            "  }",
            "",
            "  public interface CheeseStep {",
            "    Spaghetti cheese(String cheese);",
            "  }",
            "}");
  }
}
