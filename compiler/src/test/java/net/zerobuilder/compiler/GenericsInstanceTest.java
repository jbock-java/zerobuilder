package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static net.zerobuilder.compiler.test_util.GeneratedLines.GENERATED_ANNOTATION;

public class GenericsInstanceTest {

  @Test
  public void staticMethod() {
    JavaFileObject apexFactory = forSourceLines("cube.ApexFactory",
        "package cube;",
        "import net.zerobuilder.Builder;",
        "",
        "final class ApexFactory<T extends String> {",
        "  private final T string;",
        "  ApexFactory(T string) {",
        "    this.string = string;",
        "  }",
        "",
        "  @Builder",
        "  <S extends String> Apex<S> apex(S appendix) {",
        "    return new Apex(string, appendix);",
        "  }",
        "}");
    JavaFileObject apex = forSourceLines("cube.Apex",
        "package cube;",
        "",
        "final class Apex<S extends String> {",
        "  private final String string;",
        "  final S appendix;",
        "  ",
        "  Apex(String string, S appendix) {",
        "    this.string = string;",
        "    this.appendix = appendix;",
        "  }",
        "}");
    JavaFileObject expected = forSourceLines(
        "cube.ApexFactoryBuilders",
        "package cube;",
        "import javax.annotation.Generated;",
        "",
        GENERATED_ANNOTATION,
        "public final class ApexFactoryBuilders {",
        "",
        "  private ApexFactoryBuilders() {",
        "    throw new UnsupportedOperationException(\"no instances\");",
        "  }",
        "",
        "  public static <T extends String> ApexBuilder.Appendix<T> apexBuilder(ApexFactory<T> instance) {",
        "    return new ApexBuilder.Appendix(instance);",
        "  }",
        "",
        "  public static final class ApexBuilder {",
        "    private ApexBuilder() {",
        "      throw new UnsupportedOperationException(\"no instances\");",
        "    }",
        "",
        "    public static final class Appendix<T extends String> {",
        "      private final ApexFactory<T> instance;",
        "",
        "      private Appendix(ApexFactory<T> instance) {",
        "        this.instance = instance;",
        "      }",
        "",
        "      public <S extends String> Apex<S> appendix(S appendix) {",
        "        return instance.apex(appendix);",
        "      }",
        "    }",
        "  }",
        "}");
    assertAbout(javaSources()).that(ImmutableList.of(apexFactory, apex))
        .processedWith(new ZeroProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }
}
