package net.zerobuilder.examples.generics;

import org.junit.Test;

import java.util.Map.Entry;

import static net.zerobuilder.examples.generics.TestStepBuilders.entryBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TestStepTest {

  @Test
  public void entry() throws Exception {
    Entry<String, Integer> entry = entryBuilder()
        .value(12)
        .key("a")
        .suffix(12);
    assertThat(entry.getKey(), is("a12"));
    assertThat(entry.getValue(), is(12));
  }
}