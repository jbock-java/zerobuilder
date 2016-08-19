package isobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static isobuilder.compiler.GeneratedLines.GENERATED_ANNOTATION;

public class BuilderGeneratorTest {

  @Test
  public void simpleCube() {
    JavaFileObject cubeFile = forSourceLines("test.Cube",
        "package test;",
        "",
        "import isobuilder.Builder;",
        "",
        "class Cube {",
        "",
        "  @Builder",
        "  static Cube create(double height, double length, double width) {",
        "    return null;",
        "  }",
        "",
        "}");
    JavaFileObject builderFile =
        forSourceLines("test.CubeBuilder",
            "package test;",
            "",
            "import javax.annotation.Generated;",
            "",
            GENERATED_ANNOTATION,
            "public final class CubeBuilder {",
            "",
            "  private HelloWorld() {",
            "  }",
            "",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(cubeFile))
        .processedWith(new IsoProcessor())
        .compilesWithoutError()
        .and().generatesSources(builderFile);
  }

}