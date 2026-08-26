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
        "import net.zerobuilder.StepName;",
        "import java.util.List;",
        "",
        "record VibeCoder (",
        "  String name,",
        "  int age,",
        "  List<String> notes,",
        "  @StepName(\"executive\")",
        "  boolean isExecutive) {",
        "",
        "  @Builder",
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
        "import net.zerobuilder.StepName;",
        "import java.util.List;",
        "",
        "@RecordBuilder",
        "record VibeCoder (",
        "  String name,",
        "  int age,",
        "  List<String> notes,",
        "  @StepName(\"executive\")",
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
        "final class VibeCoderBuilders {",
        "",
        "  static NameStep builder() {",
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
        "import net.zerobuilder.StepName;",
        "import java.util.List;",
        "",
        "@RecordBuilder",
        "record SnailCat<E>(",
        "  E name,",
        "  List<String> notes,",
        "  @StepName(\"executive\")",
        "  boolean isExecutive) {",
        "}");
    Compilation compilation = simpleCompiler().compile(businessAnalyst);
    assertThat(compilation).succeeded();
    assertThat(compilation).generatedSourceFile("beans.SnailCatBuilders").containsLines(
        "package beans;",
        "import java.util.List;",
        "import javax.annotation.processing.Generated;",
        "",
        "final class SnailCatBuilders {",
        "",
        "  static <E> NameStep<E> builder() {",
        "    return new SnailCatBuilder<E>();",
        "  }",
        "",
        "  private static class SnailCatBuilder<E> implements NameStep<E>, NotesStep<E>, ExecutiveStep<E> {",
        "    private E name;",
        "",
        "    private List<String> notes;",
        "",
        "    SnailCatBuilder() {",
        "    }",
        "",
        "    @Override",
        "    public NotesStep<E> name(E name) {",
        "      this.name = name;",
        "      return this;",
        "    }",
        "",
        "    @Override",
        "    public ExecutiveStep<E> notes(List<String> notes) {",
        "      this.notes = notes;",
        "      return this;",
        "    }",
        "",
        "    @Override",
        "    public SnailCat<E> executive(boolean executive) {",
        "      return new SnailCat<E>(name, notes, executive);",
        "    }",
        "  }",
        "",
        "  public interface NameStep<E> {",
        "    NotesStep<E> name(E name);",
        "  }",
        "",
        "  public interface NotesStep<E> {",
        "    ExecutiveStep<E> notes(List<String> notes);",
        "  }",
        "",
        "  public interface ExecutiveStep<E> {",
        "    SnailCat<E> executive(boolean executive);",
        "  }",
        "}");
  }

  @Test
  void notRecord() {
    JavaFileObject businessAnalyst = forSourceLines("beans.VibeCoder",
        "package beans;",
        "",
        "import net.zerobuilder.RecordBuilder;",
        "",
        "@RecordBuilder",
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
