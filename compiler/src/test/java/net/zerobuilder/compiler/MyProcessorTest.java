package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static net.zerobuilder.compiler.GeneratedLines.GENERATED_ANNOTATION;

public class MyProcessorTest {

  @Test
  public void simpleCube() {
    JavaFileObject cube = forSourceLines("cube.Cube",
        "package cube;",
        "import net.zerobuilder.Build;",
        "",
        "@Build(toBuilder = true, nogc = true)",
        "abstract class Cube {",
        "  abstract double height();",
        "  abstract double length();",
        "  abstract double width();",
        "  @Build.Goal",
        "  static Cube create(double height, double length, double width) {",
        "    return null;",
        "  }",
        "}");
    JavaFileObject expected =
        forSourceLines("cube.CubeBuilders",
            "package cube;",
            "import javax.annotation.Generated;",
            "",
            GENERATED_ANNOTATION,
            "final class CubeBuilders {",
            "  private static final ThreadLocal<CubeBuilders> INSTANCE = new ThreadLocal<CubeBuilders>() {",
            "    @Override",
            "    protected CubeBuilders initialValue() {",
            "      return new CubeBuilders();",
            "    }",
            "  }",
            "",
            "  private final CubeBuilder.UpdaterImpl cubeUpdater;",
            "  private final CubeBuilder.StepsImpl cubeSteps;",
            "  private CubeBuilders() {",
            "    this.cubeUpdater = new CubeBuilder.UpdaterImpl();",
            "    this.cubeSteps = new CubeBuilder.StepsImpl();",
            "  }",
            "",
            "  static CubeBuilder.Contract.Height cubeBuilder() { return INSTANCE.get().cubeSteps; }",
            "",
            "  static CubeBuilder.Contract.CubeUpdater toBuilder(Cube cube) {",
            "    CubeBuilder.UpdaterImpl updater = INSTANCE.get().cubeUpdater;",
            "    updater.height = cube.height();",
            "    updater.length = cube.length(),",
            "    updater.width = cube.width();",
            "    return updater;",
            "  }",
            "",
            "  static final class CubeBuilder {",
            "",
            "    static final class UpdaterImpl implements",
            "          Contract.CubeUpdater {",
            "      private double height;",
            "      private double length;",
            "      private double width;",
            "      private UpdaterImpl() {}",
            "      @Override public Contract.CubeUpdater height(double height) { this.height = height; return this; }",
            "      @Override public Contract.CubeUpdater length(double length) { this.length = length; return this; }",
            "      @Override public Contract.CubeUpdater width(double width) { this.width = width; return this; }",
            "      @Override public Cube build() { return Cube.create( height, length, width ); }",
            "    }",
            "",
            "    static final class StepsImpl implements",
            "          Contract.Height, Contract.Length, Contract.Width {",
            "      private double height;",
            "      private double length;",
            "      private StepsImpl() {}",
            "      @Override public Contract.Length height(double height) { this.height = height; return this; }",
            "      @Override public Contract.Width length(double length) { this.length = length; return this; }",
            "      @Override public Cube width(double width) { return Cube.create( height, length, width ); }",
            "    }",
            "",
            "    static final class Contract {",
            "      private Contract() {}",
            "      interface CubeUpdater {",
            "        Cube build();",
            "        CubeUpdater height(double height);",
            "        CubeUpdater length(double length);",
            "        CubeUpdater width(double width);",
            "      }",
            "      interface Height { Length height(double height); }",
            "      interface Length { Width length(double length); }",
            "      interface Width { Cube width(double width); }",
            "    }",
            "  }",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(cube))
        .processedWith(new MyProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }

}