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
    JavaFileObject cube = forSourceLines("cube.Cube",
        "package cube;",
        "import com.kaputtjars.isobuilder.Build;",
        "",
        "@Build",
        "abstract class Cube {",
        "  abstract double height();",
        "  abstract double length();",
        "  abstract double width();",
        "  @Build.From",
        "  static Cube create(double height, double length, double width) {",
        "    return null;",
        "  }",
        "}");
    JavaFileObject expected =
        forSourceLines("test.CubeBuilder",
            "package test;",
            "import javax.annotation.Generated;",
            "",
            GENERATED_ANNOTATION,
            "public final class CubeBuilder {",
            "  private final UpdaterImpl updater;",
            "  private final StepsImpl steps;",
            "  private CubeBuilder() {",
            "    this.updater = new UpdaterImpl();",
            "    this.steps = new StepsImpl();",
            "  }",
            "",
            "  private static final ThreadLocal<CubeBuilder> INSTANCE = new ThreadLocal<CubeBuilder>() {",
            "    @Override",
            "    protected CubeBuilder initialValue() {",
            "      return new CubeBuilder();",
            "    }",
            "  }",
            "",
            "  static Contract.Length builder() { return INSTANCE.get().steps; }",
            "",
            "  static Contract.CubeUpdater toBuilder(Cube cube) {",
            "    UpdaterImpl updater = INSTANCE.get().updater;",
            "    updater.height(cube.height());",
            "    updater.length(cube.length()),",
            "    updater.width(cube.width());",
            "    return updater;",
            "  }",
            "",
            "  static final class UpdaterImpl implements",
            "        Contract.CubeUpdater {",
            "    private double height;",
            "    private double length;",
            "    private double width;",
            "    private UpdaterImpl() {}",
            "    @Override public Contract.CubeUpdater height(double height) { this.height = height; return this; }",
            "    @Override public Contract.CubeUpdater length(double length) { this.length = length; return this; }",
            "    @Override public Contract.CubeUpdater width(double width) { this.width = width; return this; }",
            "    @Override public Cube build() { return CubeFactory.create( height, length, width ); }",
            "  }",
            "",
            "  static final class StepsImpl implements",
            "        Contract.Height, Contract.Length, Contract.Width {",
            "    private double height;",
            "    private double length;",
            "    private double width;",
            "    private StepsImpl() {}",
            "    @Override public Contract.Length height(double height) { this.height = height; return this; }",
            "    @Override public Contract.Width length(double length) { this.length = length; return this; }",
            "    @Override public Cube width(double width) { return CubeFactory.create( height, length, width ); }",
            "  }",
            "",
            "  public static final class Contract {",
            "    private Contract() {}",
            "    public interface CubeUpdater {",
            "      Cube build();",
            "      CubeUpdater height(double height);",
            "      CubeUpdater length(double length);",
            "      CubeUpdater width(double width);",
            "    }",
            "    public interface Height { Length height(double height); }",
            "    public interface Length { Width length(double length); }",
            "    public interface Width { Cube width(double width); }",
            "  }",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(cube))
        .processedWith(new IsoProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }

}