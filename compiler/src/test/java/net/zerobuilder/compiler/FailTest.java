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
  public void twoUnnamedConstructors() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.zerobuilder.*;",
        "class Centipede {",
        "  @Builder Centipede(int a, int b) {}",
        "  @Builder Centipede(int a) {}",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Centipede", sourceLines);
    Compilation compilation = simpleCompiler().compile(javaFile);
    assertThat(compilation).failed();
    assertThat(compilation).hadErrorContaining("another goal with this name");
  }

  @Test
  public void constructorVersusFactory() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.zerobuilder.*;",
        "class Centipede {",
        "  @Builder Centipede(int a) {}",
        "  @Builder static Centipede create (int a) {}",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Centipede", sourceLines);
    Compilation compilation = simpleCompiler().compile(javaFile);
    assertThat(compilation).failed();
    assertThat(compilation).hadErrorContaining("another goal with this name");
  }

  @Test
  public void missingProjection() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.zerobuilder.*;",
        "class Bu {",
        "  final int foo = 5;",
        "  @Updater Bu(int foo, int nah) {}",
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
        "class Bu {",
        "  String getFoo() { return null; }",
        "  @Updater Bu(int foo) {}",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Bu", sourceLines);
    Compilation compilation = simpleCompiler().compile(javaFile);
    assertThat(compilation).failed();
    assertThat(compilation).hadErrorContaining("Missing projection: foo");
  }
}
