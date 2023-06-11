package net.zerobuilder.compiler;

import io.jbock.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static io.jbock.testing.compile.CompilationSubject.assertThat;
import static io.jbock.testing.compile.JavaFileObjects.forSourceLines;
import static net.zerobuilder.compiler.Compilers.simpleCompiler;

public class GenericsTest {

  @Test
  public void staticMethod() {
    JavaFileObject cube = forSourceLines("cube.Fuchur",
        "package cube;",
        "import net.zerobuilder.*;",
        "import java.util.Map;",
        "import java.util.List;",
        "import java.util.HashMap;",
        "",
        "final class Fuchur {",
        "  @Builder",
        "  static <K, V> Map<K, V> multiKey(List<K> keys, V value) {",
        "    Map<K, V> m = new HashMap<>();",
        "    for (K key : keys) {",
        "      m.put(key, value);",
        "    }",
        "    return m;",
        "  }",
        "}");
    Compilation compilation = simpleCompiler().compile(cube);
    assertThat(compilation).succeeded();
    assertThat(compilation).generatedSourceFile("cube.FuchurBuilders").containsLines(
        "package cube;",
        "import java.util.List;",
        "import java.util.Map;",
        "import javax.annotation.processing.Generated;",
        "",
        "public final class FuchurBuilders {",
        "  private FuchurBuilders() {",
        "    throw new UnsupportedOperationException(\"no instances\");",
        "  }",
        "",
        "  public static MapBuilder.Keys mapBuilder() {",
        "    return MapBuilder.keys;",
        "  }",
        "",
        "  public static final class MapBuilder {",
        "    private static final Keys keys = new Keys();",
        "",
        "    private MapBuilder() {",
        "      throw new UnsupportedOperationException(\"no instances\");",
        "    }",
        "",
        "    public static final class Keys {",
        "      private Keys() {",
        "      }",
        "      public <K> Value<K> keys(List<K> keys) {",
        "        return new Value(this, keys);",
        "      }",
        "    }",
        "",
        "    public static final class Value<K> {",
        "      private final Keys keysAcc;",
        "      private final List<K> keys;",
        "",
        "      private Value(Keys keysAcc, List<K> keys) {",
        "        this.keysAcc = keysAcc;",
        "        this.keys = keys;",
        "      }",
        "",
        "      public <V> Map<K, V> value(V value) {",
        "        return Fuchur.multiKey(keys, value);",
        "      }",
        "    }",
        "  }",
        "}");
  }
}
