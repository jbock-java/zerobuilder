package net.zerobuilder.compiler;

import io.jbock.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

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
        "record VibeCoder (",
        "  String name,",
        "  int age,",
        "  List<String> notes,",
        "  @Name(\"executive\")",
        "  boolean isExecutive) {",
        "",
        "  @Builder",
        "  @Updater",
        "  VibeCoder {",
        "  }",
        "}");
    Compilation compilation = simpleCompiler().compile(businessAnalyst);
    assertThat(compilation).succeeded();
  }

  @Test
  void recordBuilderTest() {
    JavaFileObject businessAnalyst = forSourceLines("beans.VibeCoder",
        "package beans;",
        "",
        "import net.zerobuilder.RecordBuilder;",
        "import net.zerobuilder.RecordUpdater;",
        "import net.zerobuilder.Name;",
        "import java.util.List;",
        "",
        "@RecordBuilder",
        "record VibeCoder (",
        "  String name,",
        "  int age,",
        "  List<String> notes,",
        "  @Name(\"executive\")",
        "  boolean isExecutive) {",
        "    String foo() { return null; }",
        "}");
    Compilation compilation = simpleCompiler().compile(businessAnalyst);
    assertThat(compilation).succeeded();
    assertThat(compilation).generatedSourceFile("beans.VibeCoderBuilders").containsLines(
        "package beans;",
        "import java.util.List;",
        "import javax.annotation.processing.Generated;",
        "",
        "public final class VibeCoderBuilders {",
        "",
        "  public static NameStep vibeCoderBuilder() {",
        "    return new VibeCoderBuilder();",
        "  }",
        "}");
  }

  @Test
  void genericRecordBuilderTest() {
    JavaFileObject businessAnalyst = forSourceLines("beans.VibeCoder",
        "package beans;",
        "",
        "import net.zerobuilder.RecordBuilder;",
        "import net.zerobuilder.RecordUpdater;",
        "import net.zerobuilder.Name;",
        "import net.zerobuilder.Step;",
        "import java.util.List;",
        "",
        "@RecordBuilder",
        "record SnailCat<E>(",
        "  E name,",
        "  List<String> notes,",
        "  @Name(\"executive\")",
        "  boolean isExecutive) {",
        "}");
    Compilation compilation = simpleCompiler().compile(businessAnalyst);
    assertThat(compilation).succeeded();
    assertThat(compilation).generatedSourceFile("beans.SnailCatBuilders").containsLines(
        "package beans;",
        "import java.util.List;",
        "import javax.annotation.processing.Generated;",
        "",
        "public final class SnailCatBuilders {",
        "",
        "  public static SnailCatBuilder.Name snailCatBuilder() {",
        "    return SnailCatBuilder.name;",
        "  }",
        "}");
  }

  @Test
  void notRecord() {
    JavaFileObject businessAnalyst = forSourceLines("beans.VibeCoder",
        "package beans;",
        "",
        "import net.zerobuilder.RecordUpdater;",
        "",
        "@RecordUpdater",
        "class VibeCoder {",
        "  final String name;",
        "  VibeCoder(String name) {",
        "    this.name = name;",
        "  }",
        "}");
    Compilation compilation = simpleCompiler().compile(businessAnalyst);
    assertThat(compilation).failed();
    assertThat(compilation).hadErrorContaining("Not a record type");
  }
}
