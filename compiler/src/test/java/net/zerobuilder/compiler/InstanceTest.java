package net.zerobuilder.compiler;

import io.jbock.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static io.jbock.testing.compile.CompilationSubject.assertThat;
import static io.jbock.testing.compile.JavaFileObjects.forSourceLines;
import static net.zerobuilder.compiler.Compilers.simpleCompiler;

public class InstanceTest {

  @Test
  public void instance() {
    JavaFileObject cube = forSourceLines("cube.Sum",
        "package cube;",
        "import net.zerobuilder.*;",
        "",
        "final class Sum {",
        "  private final int a;",
        "  @Builder",
        "  @GoalName(\"sum\")",
        "  int sum(int b) { return a  + b; };",
        "  Sum (int a) { this.a = a; }",
        "}");
    Compilation compilation = simpleCompiler().compile(cube);
    assertThat(compilation).succeeded();
    assertThat(compilation).generatedSourceFile("cube.SumBuilders")
        .containsLines(
            "package cube;",
            "import javax.annotation.processing.Generated;",
            "",
            "public final class SumBuilders {",
            "  private SumBuilders() {",
            "    throw new UnsupportedOperationException(\"no instances\");",
            "  }",
            "",
            "  public static SumBuilder.B sumBuilder(Sum sum) {",
            "    return new SumBuilderImpl(sum);",
            "  }",
            "",
            "  private static final class SumBuilderImpl implements SumBuilder.B {",
            "    private final Sum _sum;",
            "    SumBuilderImpl(Sum sum) {",
            "      this._sum = sum;",
            "    }",
            "    @Override",
            "    public int b(int b) {",
            "      int _integer = this._sum.sum(b);",
            "      return _integer;",
            "    }",
            "  }",
            "",
            "  public static final class SumBuilder {",
            "    private SumBuilder() {",
            "      throw new UnsupportedOperationException(\"no instances\");",
            "    }",
            "    public interface B {",
            "      int b(int b);",
            "    }",
            "  }",
            "}");
  }
}
