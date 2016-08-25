package isobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Ignore;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static isobuilder.compiler.GeneratedLines.GENERATED_ANNOTATION;

public class IsoProcessorTest {

  @Test
  public void simpleCube() {
    JavaFileObject cube = forSourceLines("cube.Cube",
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
        forSourceLines("test.CubeFactoryBuilder",
            "package test;",
            "import cube.Cube;",
            "import javax.annotation.Generated;",
            "",
            GENERATED_ANNOTATION,
            "public final class CubeFactoryBuilder {",
            "  private CubeFactoryBuilder() {}",
            "  public static Contract.CubeLength height(double height) { return new BuilderImpl(height); }",
            "",
            "  static final class BuilderImpl implements",
            "        Contract.CubeUpdater, Contract.CubeLength, Contract.CubeWidth {",
            "    private double height;",
            "    private double length;",
            "    private double width;",
            "    private BuilderImpl(double height) { this.height = height; }",
            "    @Override public Contract.CubeUpdater updateHeight(double height) { this.height = height; return this; }",
            "    @Override public Contract.CubeUpdater updateLength(double length) { this.length = length; return this; }",
            "    @Override public Contract.CubeUpdater updateWidth(double width) { this.width = width; return this; }",
            "    @Override public Contract.CubeWidth length(double length) { this.length = length; return this; }",
            "    @Override public Contract.CubeUpdater width(double width) { this.width = width; return this; }",
            "    @Override public Cube build() { return CubeFactory.create( height, length, width ); }",
            "  }",
            "",
            "  public static final class Contract {",
            "    private Contract() {}",
            "    interface CubeUpdater {",
            "      Cube build();",
            "      CubeUpdater updateHeight(double height);",
            "      CubeUpdater updateLength(double length);",
            "      CubeUpdater updateWidth(double width);",
            "    }",
            "    interface CubeLength { CubeWidth length(double length); }",
            "    interface CubeWidth { CubeUpdater width(double width); }",
            "  }",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(cube, cubeFactory))
        .processedWith(new IsoProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }

}