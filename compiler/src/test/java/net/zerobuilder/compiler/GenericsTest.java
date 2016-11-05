package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Ignore;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static net.zerobuilder.compiler.test_util.GeneratedLines.GENERATED_ANNOTATION;

@Ignore
public class GenericsTest {

  @Test
  public void instance() {
    JavaFileObject cube = forSourceLines("cube.Fuchur",
        "package cube;",
        "import net.zerobuilder.*;",
        "import java.util.Map;",
        "import java.util.List;",
        "import java.util.HashMap;",
        "",
        "@Builders",
        "final class Fuchur {",
        "  @Goal",
        "  static <K, V> Map<K, V> multiKey(List<K> keys, V value) {",
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
        "",
        "  public static MultiKeyBuilder.Keys multiKeyBuilder() {",
        "    return MultiKeyBuilderImpl.multiKeyBuilder;",
        "  }",
        "",
        "  private static final class MultiKeyBuilderImpl implements MultiKeyBuilder.Keys {",
        "",
        "    static final MultiKeyBuilderImpl multiKeyBuilder = new MultiKeyBuilderImpl();",
        "",
        "    @Override",
        "    public <K> MultiKeyBuilder.Value<K> keys(List<K> keys) {",
        "      return new ValueImpl<>(keys);",
        "    }",
        "",
        "    private static final class ValueImpl<K> implements MultiKeyBuilder.Value<K> {",
        "",
        "      private final List<K> keys;",
        "",
        "      private ValueImpl(List<K> keys) {",
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
        "  public static final class MultiKeyBuilder {",
        "",
        "    public interface Keys {",
        "      <K> Value<K> keys(List<K> keys);",
        "    }",
        "",
        "    public interface Value<K> {",
        "      <V> Map<K, V> value(V value);",
        "    }",
        "",
        "    private MultiKeyBuilder() {",
        "      throw new UnsupportedOperationException(\"no instances\");",
        "    }",
        "  }",
        "",
        "  private FuchurBuilders() {",
        "    throw new UnsupportedOperationException(\"no instances\");",
        "  }",
        "}");
    assertAbout(javaSources()).that(ImmutableList.of(cube))
        .processedWith(new ZeroProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }
}
