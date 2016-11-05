package net.zerobuilder.compiler.examples;

import java.util.List;
import java.util.Map;

public final class TrickyGenericsBuilders {

  public static GetListBuilder.Keys getListBuilder() {
    return GetListBuilderImpl.getListBuilder;
  }

  private static final class GetListBuilderImpl implements GetListBuilder.Keys {

    static final GetListBuilderImpl getListBuilder = new GetListBuilderImpl();

    @Override
    public <K> GetListBuilder.Value<K> keys(List<K> keys) {
      return new ValueImpl<>(keys);
    }

    private static final class ValueImpl<K> implements GetListBuilder.Value<K> {

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

  public static final class GetListBuilder {

    public interface Keys {
      <K> Value<K> keys(List<K> keys);
    }

    public interface Value<K> {
      <V> Map<K, V> value(V value);
    }

    private GetListBuilder() {
      throw new UnsupportedOperationException("no instances");
    }
  }

  private TrickyGenericsBuilders() {
    throw new UnsupportedOperationException("no instances");
  }
}
