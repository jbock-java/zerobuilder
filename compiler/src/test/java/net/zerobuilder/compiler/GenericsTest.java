package net.zerobuilder.compiler;

import io.jbock.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static io.jbock.testing.compile.CompilationSubject.assertThat;
import static io.jbock.testing.compile.JavaFileObjects.forSourceLines;
import static net.zerobuilder.compiler.Compilers.simpleCompiler;

class GenericsTest {

  @Test
  void genericsTest() {
    JavaFileObject cube = forSourceLines("cube.Fuchur",
        "package cube;",
        "import net.zerobuilder.RecordBuilder;",
        "import java.util.Map;",
        "import java.util.List;",
        "import java.util.HashMap;",
        "",
        "@RecordBuilder",
        "record Fuchur<K, V>(",
        "  List<K> keys,",
        "  V value) {",
        "}");
    Compilation compilation = simpleCompiler().compile(cube);
    assertThat(compilation).succeeded();
    assertThat(compilation).generatedSourceFile("cube.FuchurBuilders").containsLines(
        "package cube;",
        "import java.util.List;",
        "import javax.annotation.processing.Generated;",
        "",
        "final class FuchurBuilders {",
        "",
        "  static <K, V> KeysStep<K, V> builder() {",
        "    return new FuchurBuilder<K, V>();",
        "  }",
        "",
        "  private static class FuchurBuilder<K, V> implements KeysStep<K, V>, ValueStep<K, V> {",
        "    private List<K> keys;",
        "",
        "    @Override",
        "    public ValueStep<K, V> keys(List<K> keys) {",
        "      this.keys = keys;",
        "      return this;",
        "    }",
        "",
        "    @Override",
        "    public Fuchur<K, V> value(V value) {",
        "      return new Fuchur<K, V>(keys, value);",
        "    }",
        "  }",
        "}");
  }
}
