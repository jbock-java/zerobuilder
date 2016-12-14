package net.zerobuilder.examples.generics;

import org.junit.Test;

import java.util.Map;

public class GenericInstanceTest {

  @Test(expected = NullPointerException.class)
  public void nullBuilder() throws Exception {
    GenericInstance<String> instance = new GenericInstance<>();
    Map.Entry<String, String> d =
        GenericInstanceBuilders.entryBuilder(instance)
            .suffix("A")
            .key("key").value(null);
  }
}