package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;
import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static java.util.Collections.singletonList;

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
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new ZeroProcessor())
        .failsToCompile()
        .withErrorContaining("another goal with this name")
        .in(javaFile);
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
    assertAbout(javaSources()).that(ImmutableList.of(javaFile))
        .processedWith(new ZeroProcessor())
        .failsToCompile()
        .withErrorContaining("another goal with this name")
        .in(javaFile);
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
    assertAbout(javaSources()).that(ImmutableList.of(javaFile))
        .processedWith(new ZeroProcessor())
        .failsToCompile()
        .withErrorContaining("Missing projection: nah")
        .in(javaFile);
  }
}
