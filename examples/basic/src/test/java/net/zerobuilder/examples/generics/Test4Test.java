package net.zerobuilder.examples.generics;

import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import static java.util.Arrays.asList;
import static net.zerobuilder.examples.generics.Test4Builders.entryBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class Test4Test {

  @Test
  public void entry() throws Exception {
    Entry<String, Integer> entry = entryBuilder()
        .key("a")
        .value(1)
        .ks(asList("b", "c"))
        .value2(25);
    assertThat(entry.getKey(), is("abc"));
    assertThat(entry.getValue(), is(26));
  }
}