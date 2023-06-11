package net.zerobuilder.compiler;

import io.jbock.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static io.jbock.testing.compile.CompilationSubject.assertThat;
import static io.jbock.testing.compile.JavaFileObjects.forSourceLines;
import static net.zerobuilder.compiler.Compilers.simpleCompiler;

public class RecyclingInstanceTest {

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
        "  @Recycle",
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
            "  private static final ThreadLocal<SumBuilderImpl> sumBuilderImpl = new ThreadLocal<SumBuilderImpl>() {",
            "    @Override",
            "    protected SumBuilderImpl initialValue() {",
            "      return new SumBuilderImpl();",
            "    }",
            "  };",
            "",
            "  private SumBuilders() {",
            "    throw new UnsupportedOperationException(\"no instances\");",
            "  }",
            "",
            "  public static SumBuilder.B sumBuilder(Sum sum) {",
            "    SumBuilderImpl _builder = sumBuilderImpl.get();",
            "    if (_builder._currently_in_use) {",
            "      sumBuilderImpl.remove();",
            "      _builder = sumBuilderImpl.get();",
            "    }",
            "    _builder._currently_in_use = true;",
            "    _builder._sum = sum;",
            "    return _builder;",
            "  }",
            "",
            "  private static final class SumBuilderImpl implements SumBuilder.B {",
            "    private Sum _sum;",
            "    private boolean _currently_in_use;",
            "    SumBuilderImpl() {",
            "    }",
            "    @Override",
            "    public int b(int b) {",
            "      this._currently_in_use = false;",
            "      int _integer = this._sum.sum(b);",
            "      this._sum = null;",
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
