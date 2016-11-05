package net.zerobuilder.compiler.examples;

import java.util.List;
import java.util.Map;

public final class FuchurBuilders {
  
  public static MultiKeyBuilder.Keys multiKeyBuilder() {
    return MultiKeyBuilderImpl.multiKeyBuilder;
  }

  private static final class MultiKeyBuilderImpl implements MultiKeyBuilder.Keys {

    static final MultiKeyBuilderImpl multiKeyBuilder = new MultiKeyBuilderImpl();

    @Override
    public <K> MultiKeyBuilder.Value<K> keys(List<K> keys) {
      return new ValueImpl<>(keys);
    }

    private static final class ValueImpl<K> implements MultiKeyBuilder.Value<K> {

      private final List<K> keys;

      private ValueImpl(List<K> keys) {
        this.keys = keys;
      }

      @Override
      public <V> Map<K, V> value(V value) {
        return Fuchur.multiKey(keys, value);
      }
    }
  }

  public static final class MultiKeyBuilder {

    public interface Keys {
      <K> Value<K> keys(List<K> keys);
    }

    public interface Value<K> {
      <V> Map<K, V> value(V value);
    }

    private MultiKeyBuilder() {
      throw new UnsupportedOperationException("no instances");
    }
  }

  private FuchurBuilders() {
    throw new UnsupportedOperationException("no instances");
  }
}
