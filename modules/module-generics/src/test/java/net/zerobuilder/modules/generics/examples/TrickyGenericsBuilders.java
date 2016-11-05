package net.zerobuilder.modules.generics.examples;

import java.util.List;
import java.util.Map;

public final class TrickyGenericsBuilders {

  public static GetListBuilder.Source getListBuilder() {
    return GetListBuilderImpl.getListBuilder;
  }

  private static final class GetListBuilderImpl implements GetListBuilder.Source {

    static final GetListBuilderImpl getListBuilder = new GetListBuilderImpl();

    @Override
    public <K, V> GetListBuilder.Key<K, V> source(Map<K, List<V>> source) {
      return new KeyImpl(source);
    }

    private static final class KeyImpl<K, V> implements GetListBuilder.Key<K, V> {

      private final Map<K, List<V>> source;

      private KeyImpl(Map<K, List<V>> source) {
        this.source = source;
      }

      @Override
      public GetListBuilder.DefaultValue<V> key(K key) {
        return new DefaultValueImpl(source, key);
      }
    }

    private static final class DefaultValueImpl<K, V> implements GetListBuilder.DefaultValue<V> {

      private final Map<K, List<V>> source;
      private final K key;

      private DefaultValueImpl(Map<K, List<V>> source, K key) {
        this.source = source;
        this.key = key;
      }

      @Override
      public List<V> defaultValue(V value) {
        return TrickyGenerics.getList(source, key, value);
      }
    }
  }

  public static final class GetListBuilder {

    public interface Source {
      <K, V> Key<K, V> source(Map<K, List<V>> source);
    }

    public interface Key<K, V> {
      DefaultValue<V> key(K key);
    }

    public interface DefaultValue<V> {
      List<V> defaultValue(V defaultValue);
    }

    private GetListBuilder() {
      throw new UnsupportedOperationException("no instances");
    }
  }

  private TrickyGenericsBuilders() {
    throw new UnsupportedOperationException("no instances");
  }
}
