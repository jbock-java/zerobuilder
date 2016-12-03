package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static net.zerobuilder.compiler.test_util.GeneratedLines.GENERATED_ANNOTATION;

public class ThrowTest {

  @Test
  public void instance() {
    JavaFileObject cube = forSourceLines("cube.Throw",
        "package cube;",
        "import net.zerobuilder.*;",
        "import java.io.IOException;",
        "",
        "final class Throw {",
        "  @Builder",
        "  static void doUpdate(String message) throws IOException {",
        "    throw new IOException();",
        "  }",
        "}");
    JavaFileObject expected =
        forSourceLines("cube.ThrowBuilders",
            "package cube;",
            "import java.io.IOException;",
            "import javax.annotation.Generated;",
            "",
            GENERATED_ANNOTATION,
            "public final class ThrowBuilders {",
            "  private ThrowBuilders() {",
            "    throw new UnsupportedOperationException(\"no instances\");",
            "  }",
            "",
            "  public static VoidBuilder.Message VoidBuilder() {",
            "    return new VoidBuilderImpl();",
            "  }",
            "",
            "  private static final class VoidBuilderImpl implements VoidBuilder.Message {",
            "    VoidBuilderImpl() {}",
            "    @Override public void message(String message) throws IOException {",
            "      Throw.doUpdate(message);",
            "    }",
            "  }",
            "",
            "  public static final class VoidBuilder {",
            "    private VoidBuilder() {",
            "      throw new UnsupportedOperationException(\"no instances\");",
            "    }",
            "    public interface Message {",
            "      void message(String message) throws IOException;",
            "    }",
            "  }",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(cube))
        .processedWith(new ZeroProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }
}
