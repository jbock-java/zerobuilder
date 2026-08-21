package net.zerobuilder.compiler;

import io.jbock.testing.compile.Compilation;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

import static io.jbock.testing.compile.CompilationSubject.assertThat;
import static io.jbock.testing.compile.JavaFileObjects.forSourceLines;
import static net.zerobuilder.compiler.Compilers.simpleCompiler;

class RecordTest {

  @Test
  void recordTest() {
    JavaFileObject businessAnalyst = forSourceLines("beans.VibeCoder",
        "package beans;",
        "",
        "import net.zerobuilder.Builder;",
        "import net.zerobuilder.Updater;",
        "import net.zerobuilder.Name;",
        "import java.util.List;",
        "",
        "public record VibeCoder (",
        "  String name,",
        "  int age,",
        "  List<String> notes,",
        "  @Name(\"executive\")",
        "  boolean isExecutive) {",
        "",
        "  @Builder",
        "  @Updater",
        "  public VibeCoder {",
        "  }",
        "}");
    Compilation compilation = simpleCompiler().compile(businessAnalyst);
    assertThat(compilation).succeeded();
  }
}
