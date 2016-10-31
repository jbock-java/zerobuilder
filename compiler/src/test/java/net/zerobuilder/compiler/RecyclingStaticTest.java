package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static net.zerobuilder.compiler.GeneratedLines.GENERATED_ANNOTATION;

public class RecyclingStaticTest {

  @Test
  public void simpleCube() {
    JavaFileObject cube = forSourceLines("cube.Cube",
        "package cube;",
        "import net.zerobuilder.*;",
        "import java.util.List;",
        "",
        "@Builders(recycle = true)",
        "abstract class Cube {",
        "  abstract double height();",
        "  abstract List<String> length();",
        "  abstract String width();",
        "  @Goal(name = \"cuboid\", updater = true, updaterAccess = AccessLevel.PACKAGE)",
        "  static Cube create(double height, List<String> length, @Step(nullPolicy = NullPolicy.REJECT) String width) {",
        "    return null;",
        "  }",
        "}");
    JavaFileObject expected =
        forSourceLines("cube.CubeBuilders",
            "package cube;",
            "import java.util.Collections;",
            "import java.util.List;",
            "import javax.annotation.Generated;",
            "",
            GENERATED_ANNOTATION,
            "public final class CubeBuilders {",
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
            "  static CuboidUpdater cuboidUpdater(Cube cube) {",
            "    if (cube.width() == null) {",
            "      throw new NullPointerException(\"width\");",
            "    }",
            "    CuboidUpdater updater = INSTANCE.get().cuboidUpdater;",
            "    updater.height = cube.height();",
            "    updater.length = cube.length(),",
            "    updater.width = cube.width();",
            "    return updater;",
            "  }",
            "",
            "  public static CuboidBuilder.Height cuboidBuilder() {",
            "    CuboidBuilderImpl cuboidBuilderImpl = INSTANCE.get().cuboidBuilderImpl;",
            "    return cuboidBuilderImpl;",
            "  }",
            "",
            "  public static final class CuboidUpdater {",
            "    private double height;",
            "    private List<String> length;",
            "    private String width;",
            "    private CuboidUpdater() {}",
            "    public CuboidUpdater height(double height) { this.height = height; return this; }",
            "    public CuboidUpdater length(List<String> length) { this.length = length; return this; }",
            "    public CuboidUpdater emptyLength() {",
            "      this.length = Collections.emptyList();",
            "      return this;",
            "    }",
            "    public CuboidUpdater width(String width) {",
            "      if (width == null) {",
            "        throw new NullPointerException(\"width\");",
            "      }",
            "      this.width = width;",
            "      return this;",
            "    }",
            "    public Cube done() { return Cube.create( height, length, width ); }",
            "  }",
            "",
            "  private static final class CuboidBuilderImpl implements",
            "        CuboidBuilder.Height, CuboidBuilder.Length, CuboidBuilder.Width {",
            "    private double height;",
            "    private List<String> length;",
            "    StepsImpl() {}",
            "    @Override public CuboidBuilder.Length height(double height) { this.height = height; return this; }",
            "    @Override public CuboidBuilder.Width length(List<String> length) { this.length = length; return this; }",
            "    @Override public CuboidBuilder.Width emptyLength() {",
            "      this.length = Collections.emptyList();",
            "      return this;",
            "    }",
            "    @Override public Cube width(String width) {",
            "      if (width == null) {",
            "        throw new NullPointerException(\"width\");",
            "      }",
            "      return Cube.create( height, length, width );",
            "    }",
            "  }",
            "",
            "  public static final class CuboidBuilder {",
            "    private CuboidBuilder() {",
            "      throw new UnsupportedOperationException(\"no instances\");",
            "    }",
            "    public interface Height { Length height(double height); }",
            "    public interface Length {",
            "      Width length(List<String> length);",
            "      Width emptyLength();",
            "    }",
            "    public interface Width { Cube width(String width); }",
            "  }",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(cube))
        .processedWith(new ZeroProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }

}
