package isobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static isobuilder.compiler.GeneratedLines.GENERATED_ANNOTATION;

public class IsoProcessorTest {

  @Test
  public void simpleCube() {
    JavaFileObject cube = forSourceLines("test.Cube",
        "package cube;",
        "",
        "import isobuilder.Builder;",
        "",
        "public class Cube {",
        "}");
    JavaFileObject cubeFactory = forSourceLines("test.CubeFactory",
        "package test;",
        "",
        "import isobuilder.Builder;",
        "import cube.Cube;",
        "",
        "class CubeFactory {",
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
            "  private CubeBuilder() {",
            "  }",
            "",
            "}");
    JavaFileObject contractFile =
        forSourceLines("test.CubeBuilderContract",
            "package test;",
            "",
            "import javax.annotation.Generated;",
            "",
            GENERATED_ANNOTATION,
            "public final class CubeBuilderContract {",
            "",
            "  private CubeBuilderContract() {",
            "  }",
            "",
            "  interface CubeUpdater {",
            "    CubeUpdater updateHeight(double height);",
            "    CubeUpdater updateLength(double length);",
            "    CubeUpdater updateWidth(double width);",
            "  }",
            "",
            "  interface WidthStep {",
            "    CubeUpdater width(double width);",
            "  }",
            "",
            "  interface LengthStep {",
            "    WidthStep length(double length);",
            "  }",
            "",
            "  interface HeightStep {",
            "    LengthStep height(double height);",
            "  }",
            "",
            "  interface CubeContract extends CubeUpdater, WidthStep, LengthStep, HeightStep {",
            "  }",
            "",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(cube, cubeFactory))
        .processedWith(new IsoProcessor())
        .compilesWithoutError()
        .and().generatesSources(builderFile, contractFile);
  }

}