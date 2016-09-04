package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public class FailTest {

  @Test
  public void goalNotInBuild() {
    String subject = "  @Goal static void create(int numberOfLegs) {}";
    ImmutableList<String> sourceLines = ImmutableList.of("package test;",
        "import net.zerobuilder.Goal;",
        "class Centipede {",
        subject,
        "}");
    JavaFileObject javaFile = forSourceLines("test.Centipede", sourceLines);
    int line = sourceLines.indexOf(subject);
    assertAbout(javaSources()).that(ImmutableList.of(javaFile))
        .processedWith(new ZeroProcessor())
        .failsToCompile()
        .withErrorContaining("outside")
        .in(javaFile)
        .onLine(line + 1);
  }

}
