package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static net.zerobuilder.compiler.GeneratedLines.GENERATED_ANNOTATION;

public class ZeroProcessorTest {

  @Test
  public void simpleCube() {
    JavaFileObject cube = forSourceLines("cube.Cube",
        "package cube;",
        "import net.zerobuilder.*;",
        "",
        "@Builders(recycle = true)",
        "abstract class Cube {",
        "  abstract double height();",
        "  abstract double length();",
        "  abstract double width();",
        "  @Goal(name = \"cuboid\", toBuilder = true)",
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
            "  private final CuboidUpdater cuboidUpdater = new CuboidUpdater();",
            "  private final CuboidBuilderImpl cuboidBuilderImpl = new CuboidBuilderImpl();",
            "  private CubeBuilders() {}",
            "",
            "  static CuboidBuilder.Height cuboidBuilder() {",
            "    CuboidBuilderImpl cuboidBuilderImpl = INSTANCE.get().cuboidBuilderImpl;",
            "    return cuboidBuilderImpl;",
            "  }",
            "",
            "  static CuboidUpdater cuboidToBuilder(Cube cube) {",
            "    CuboidUpdater updater = INSTANCE.get().cuboidUpdater;",
            "    updater.height = cube.height();",
            "    updater.length = cube.length(),",
            "    updater.width = cube.width();",
            "    return updater;",
            "  }",
            "",
            "  static final class CuboidUpdater {",
            "    private double height;",
            "    private double length;",
            "    private double width;",
            "    private CuboidUpdater() {}",
            "    CuboidUpdater height(double height) { this.height = height; return this; }",
            "    CuboidUpdater length(double length) { this.length = length; return this; }",
            "    CuboidUpdater width(double width) { this.width = width; return this; }",
            "    Cube build() { return Cube.create( height, length, width ); }",
            "  }",
            "",
            "  static final class CuboidBuilderImpl implements",
            "        CuboidBuilder.Height, CuboidBuilder.Length, CuboidBuilder.Width {",
            "    private double height;",
            "    private double length;",
            "    private StepsImpl() {}",
            "    @Override public CuboidBuilder.Length height(double height) { this.height = height; return this; }",
            "    @Override public CuboidBuilder.Width length(double length) { this.length = length; return this; }",
            "    @Override public Cube width(double width) { return Cube.create( height, length, width ); }",
            "  }",
            "",
            "  static final class CuboidBuilder {",
            "    private CuboidBuilder() {}",
            "    interface Height { Length height(double height); }",
            "    interface Length { Width length(double length); }",
            "    interface Width { Cube width(double width); }",
            "  }",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(cube))
        .processedWith(new ZeroProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }

}
