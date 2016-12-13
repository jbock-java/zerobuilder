package net.zerobuilder.examples.generics;

import net.zerobuilder.examples.generics.GenericInstanceBuilders.EntryBuilder;
import org.junit.Test;

public class GenericInstanceTest {

  @Test(expected = NullPointerException.class)
  public void entry() throws Exception {
    GenericInstance<String> instance = new GenericInstance<>();
    EntryBuilder.Suffix<String> stringSuffix = GenericInstanceBuilders.entryBuilder(instance);
    EntryBuilder.Key<Object, Object> a = stringSuffix
        .suffix("A");
    EntryBuilder.Value<Object, Object> key = a
        .key("key");
  }

}