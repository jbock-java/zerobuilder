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
        "import isobuilder.Builder;",
        "public class Cube {}");
    JavaFileObject cubeFactory = forSourceLines("test.CubeFactory",
        "package test;",
        "import isobuilder.Builder;",
        "import cube.Cube;",
        "",
        "class CubeFactory {",
        "  @Builder",
        "  static Cube create(double height, double length, double width) {",
        "    return null;",
        "  }",
        "",
        "}");
    JavaFileObject expected =
        forSourceLines("test.CubeFactory_IsoBuilder",
            "package test;",
            "import javax.annotation.Generated;",
            GENERATED_ANNOTATION,
            "",
            "public final class CubeFactory_IsoBuilder {",
            "  private CubeFactory_IsoBuilder() {}",
            "",
            "  public static final class Contract {",
            "    private Contract() {}",
            "    interface CubeUpdater {",
            "      CubeUpdater updateHeight(double height);",
            "      CubeUpdater updateLength(double length);",
            "      CubeUpdater updateWidth(double width);",
            "    }",
            "    interface CubeWidth { CubeUpdater width(double width); }",
            "    interface CubeLength { CubeWidth length(double length); }",
            "    interface CubeHeight { CubeLength height(double height); }",
            "  }",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(cube, cubeFactory))
        .processedWith(new IsoProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }

}