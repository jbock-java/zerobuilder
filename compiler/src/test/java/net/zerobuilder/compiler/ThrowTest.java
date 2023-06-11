package net.zerobuilder.compiler;

import io.jbock.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static io.jbock.testing.compile.CompilationSubject.assertThat;
import static io.jbock.testing.compile.JavaFileObjects.forSourceLines;
import static net.zerobuilder.compiler.Compilers.simpleCompiler;

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
    Compilation compilation = simpleCompiler().compile(cube);
    assertThat(compilation).succeeded();
    assertThat(compilation).generatedSourceFile("cube.ThrowBuilders")
        .containsLines(
            "package cube;",
            "import java.io.IOException;",
            "import javax.annotation.processing.Generated;",
            "",
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
            "    VoidBuilderImpl() {",
            "    }",
            "    @Override",
            "    public void message(String message) throws IOException {",
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
  }
}
