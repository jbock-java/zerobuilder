package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static net.zerobuilder.compiler.GeneratedLines.GENERATED_ANNOTATION;

public class ThrowTest {

  @Test
  public void instance() {
    JavaFileObject cube = forSourceLines("cube.Sum",
        "package cube;",
        "import net.zerobuilder.*;",
        "import java.io.IOException;",
        "",
        "@Builders",
        "final class Throw {",
        "  @Goal static void doUpdate(String message) { throw new IOException(); };",
        "}");
    JavaFileObject expected =
        forSourceLines("cube.ThrowBuilders",
            "package cube;",
            "import javax.annotation.Generated;",
            "",
            GENERATED_ANNOTATION,
            "public final class ThrowBuilders {",
            "  private SumBuilders() {",
            "    throw new UnsupportedOperationException(\"no instances\");",
            "  }",
            "",
            "  public static SumBuilder.B sumBuilder(Sum sum) {",
            "    SumBuilderImpl sumBuilderImpl = new SumBuilderImpl(sum);",
            "    return sumBuilderImpl;",
            "  }",
            "",
            "  static final class SumBuilderImpl implements SumBuilder.B {",
            "    private final Sum _sum;",
            "    private StepsImpl(Sum sum) { this._sum = sum; }",
            "    @Override public int b(int b) {",
            "      return this._sum.sum( b );",
            "    }",
            "  }",
            "",
            "  public static final class SumBuilder {",
            "    private SumBuilder() {",
            "      throw new UnsupportedOperationException(\"no instances\");",
            "    }",
            "    public interface B { int b(int b); }",
            "  }",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(cube))
        .processedWith(new ZeroProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }
}
