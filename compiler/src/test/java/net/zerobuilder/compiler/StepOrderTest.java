package net.zerobuilder.compiler;

import io.jbock.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static io.jbock.testing.compile.CompilationSubject.assertThat;
import static io.jbock.testing.compile.JavaFileObjects.forSourceLines;
import static net.zerobuilder.compiler.Compilers.simpleCompiler;

class StepOrderTest {

  @Test
  void instance() {
    JavaFileObject cube = forSourceLines("cube.Spaghetti",
        "package cube;",
        "import net.zerobuilder.Builder;",
        "import net.zerobuilder.StepOrder;",
        "",
        "final class Spaghetti {",
        "  final String cheese;",
        "  final String sauce;",
        "  @Builder",
        "  Spaghetti(String cheese, @StepOrder(0) String sauce) {",
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
            "final class SpaghettiBuilders {",
            "  private SpaghettiBuilders() {",
            "  }",
            "",
            "  static SauceStep builder() {",
            "    return new SpaghettiBuilder();",
            "  }",
            "",
            "  private static class SpaghettiBuilder implements SauceStep, CheeseStep {",
            "    private String sauce;",
            "",
            "    SpaghettiBuilder() {",
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
