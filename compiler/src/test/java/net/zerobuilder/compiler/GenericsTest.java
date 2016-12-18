package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static net.zerobuilder.compiler.test_util.GeneratedLines.GENERATED_ANNOTATION;

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
    JavaFileObject expected = forSourceLines(
        "cube.FuchurBuilders",
        "package cube;",
        "import java.util.List;",
        "import java.util.Map;",
        "import javax.annotation.Generated;",
        "",
        GENERATED_ANNOTATION,
        "public final class FuchurBuilders {",
        "",
        "  private FuchurBuilders() {",
        "    throw new UnsupportedOperationException(\"no instances\");",
        "  }",
        "",
        "  public static MapBuilder.Keys mapBuilder() {",
        "    return MapBuilderImpl.keysImpl;",
        "  }",
        "",
        "  private static final class MapBuilderImpl {",
        "",
        "    private static final KeysImpl keysImpl = new KeysImpl();",
        "",
        "    private MapBuilderImpl() {",
        "      throw new UnsupportedOperationException(\"no instances\");",
        "    }",
        "",
        "    private static final class KeysImpl implements MapBuilder.Keys {",
        "",
        "      @Override",
        "      public <K> MapBuilder.Value<K> keys(List<K> keys) {",
        "        return new ValueImpl(this, keys);",
        "      }",
        "    }",
        "",
        "    private static final class ValueImpl<K> implements MapBuilder.Value<K> {",
        "      private final KeysImpl keysImpl;",
        "      private final List<K> keys;",
        "",
        "      ValueImpl(KeysImpl keysImpl, List<K> keys) {",
        "        this.keysImpl = keysImpl;",
        "        this.keys = keys;",
        "      }",
        "",
        "      @Override",
        "      public <V> Map<K, V> value(V value) {",
        "        return Fuchur.multiKey(keys, value);",
        "      }",
        "    }",
        "  }",
        "",
        "  public static final class MapBuilder {",
        "",
        "    private MapBuilder() {",
        "      throw new UnsupportedOperationException(\"no instances\");",
        "    }",
        "",
        "    public interface Keys {",
        "      <K> Value<K> keys(List<K> keys);",
        "    }",
        "",
        "    public interface Value<K> {",
        "      <V> Map<K, V> value(V value);",
        "    }",
        "  }",
        "}");
    assertAbout(javaSources()).that(ImmutableList.of(cube))
        .processedWith(new ZeroProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }
}
