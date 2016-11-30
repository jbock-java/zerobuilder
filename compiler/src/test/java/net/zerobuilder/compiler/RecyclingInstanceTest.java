package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static net.zerobuilder.compiler.test_util.GeneratedLines.GENERATED_ANNOTATION;

public class RecyclingInstanceTest {

  @Test
  public void instance() {
    JavaFileObject cube = forSourceLines("cube.Sum",
        "package cube;",
        "import net.zerobuilder.*;",
        "",
        "@Builders(recycle = true)",
        "final class Sum {",
        "  private final int a;",
        "  @Goal(name = \"sum\") int sum(int b) { return a  + b; };",
        "  Sum (int a) { this.a = a; }",
        "}");
    JavaFileObject expected =
        forSourceLines("cube.SumBuilders",
            "package cube;",
            "import javax.annotation.Generated;",
            "",
            GENERATED_ANNOTATION,
            "public final class SumBuilders {",
            "  private static final ThreadLocal<SumBuilderImpl> sumBuilderImpl = new ThreadLocal<SumBuilderImpl>() {",
            "    @Override",
            "    protected SumBuilderImpl initialValue() {",
            "      return new SumBuilderImpl();",
            "    }",
            "  }",
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
            "    StepsImpl() {}",
            "    @Override public int b(int b) {",
            "      this._currently_in_use = false;",
            "      int _integer = this._sum.sum( b );",
            "      this._sum = null;",
            "      return _integer;",
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
