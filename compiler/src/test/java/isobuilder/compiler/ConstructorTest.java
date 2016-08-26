package isobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static isobuilder.compiler.GeneratedLines.GENERATED_ANNOTATION;

public class ConstructorTest {

  @Test
  public void simpleCube() {
    JavaFileObject cube = forSourceLines("test.Cube",
        "package test;",
        "import isobuilder.Builder;",
        "",
        "class Cube {",
        "  @Builder",
        "  Cube(double height, double length, double width) {}",
        "",
        "}");
    JavaFileObject expected =
        forSourceLines("test.CubeBuilder",
            "package test;",
            "import javax.annotation.Generated;",
            "",
            GENERATED_ANNOTATION,
            "public final class CubeBuilder {",
            "  private CubeBuilder() {}",
            "  public static Contract.Length height(double height) { return new BuilderImpl(height); }",
            "",
            "  static final class BuilderImpl implements",
            "        Contract.CubeUpdater, Contract.Length, Contract.Width {",
            "    private double height;",
            "    private double length;",
            "    private double width;",
            "    private BuilderImpl(double height) { this.height = height; }",
            "    @Override public Contract.CubeUpdater updateHeight(double height) { this.height = height; return this; }",
            "    @Override public Contract.CubeUpdater updateLength(double length) { this.length = length; return this; }",
            "    @Override public Contract.CubeUpdater updateWidth(double width) { this.width = width; return this; }",
            "    @Override public Contract.Width length(double length) { this.length = length; return this; }",
            "    @Override public Contract.CubeUpdater width(double width) { this.width = width; return this; }",
            "    @Override public Cube build() { return new Cube( height, length, width ); }",
            "  }",
            "",
            "  public static final class Contract {",
            "    private Contract() {}",
            "    public interface CubeUpdater {",
            "      Cube build();",
            "      CubeUpdater updateHeight(double height);",
            "      CubeUpdater updateLength(double length);",
            "      CubeUpdater updateWidth(double width);",
            "    }",
            "    public interface Length { Width length(double length); }",
            "    public interface Width { CubeUpdater width(double width); }",
            "  }",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(cube))
        .processedWith(new IsoProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }

}