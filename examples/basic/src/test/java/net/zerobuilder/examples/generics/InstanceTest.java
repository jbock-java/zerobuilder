package net.zerobuilder.examples.generics;

import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class InstanceTest {

  @Test
  public void entry() throws Exception {
    Instance<String> instance = new Instance<>("");
    Map.Entry<Integer, String> entry = InstanceBuilders.entryBuilder(instance)
        .key(1)
        .value("y");
    assertThat(entry.getKey(), is(1));
    assertThat(entry.getValue(), is("y"));
  }

  @Test
  public void ventry() throws Exception {
    Instance<String> instance = new Instance<>("");
    Map.Entry entry = InstanceBuilders.ventryBuilder(instance)
        .value("1");
    assertThat(entry.getKey(), is("1"));
    assertThat(entry.getValue(), is("1"));
  }
}