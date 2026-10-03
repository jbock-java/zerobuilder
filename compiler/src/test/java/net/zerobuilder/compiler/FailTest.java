package net.zerobuilder.compiler;

import io.jbock.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.util.Arrays;
import java.util.List;

import static io.jbock.testing.compile.CompilationSubject.assertThat;
import static io.jbock.testing.compile.JavaFileObjects.forSourceLines;
import static net.zerobuilder.compiler.Compilers.simpleCompiler;

public class FailTest {

  @Test
  public void missingProjection() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.zerobuilder.RecordBuilder;",
        "@RecordBuilder",
        "class Bu {",
        "  final int foo = 5;",
        "  Bu(int foo, int nah) {}",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Bu", sourceLines);
    Compilation compilation = simpleCompiler().compile(javaFile);
    assertThat(compilation).failed();
    assertThat(compilation).hadErrorContaining("Missing projection: nah");
  }

  @Test
  public void projectionWrongType() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.zerobuilder.*;",
        "@RecordBuilder",
        "class Bu {",
        "  String foo() { return null; }",
        "  Bu(int foo) {}",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Bu", sourceLines);
    Compilation compilation = simpleCompiler().compile(javaFile);
    assertThat(compilation).failed();
    assertThat(compilation).hadErrorContaining("Missing projection: foo");
  }

  @Test
  public void twoConstructors() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.zerobuilder.RecordBuilder;",
        "@RecordBuilder",
        "class Bu {",
        "  String foo() { return null; }",
        "  Bu(String foo) {}",
        "  Bu() {}",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Bu", sourceLines);
    Compilation compilation = simpleCompiler().compile(javaFile);
    assertThat(compilation).failed();
    assertThat(compilation).hadErrorContaining("more than one constructor found");
  }

  @Test
  public void checkedExceptionConstructor() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.zerobuilder.RecordBuilder;",
        "@RecordBuilder",
        "class Bu {",
        "  String foo() { return null; }",
        "  Bu(String foo) throws java.io.IOException {}",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Bu", sourceLines);
    Compilation compilation = simpleCompiler().compile(javaFile);
    assertThat(compilation).failed();
    assertThat(compilation).hadErrorContaining("Checked exception is not allowed here.");
  }

  @Test
  public void checkedExceptionAccessor() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.zerobuilder.RecordBuilder;",
        "@RecordBuilder",
        "class Bu {",
        "  String foo() throws java.io.IOException { return null; }",
        "  Bu(String foo) {}",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Bu", sourceLines);
    Compilation compilation = simpleCompiler().compile(javaFile);
    assertThat(compilation).failed();
    assertThat(compilation).hadErrorContaining("Checked exception is not allowed here.");
  }
}
