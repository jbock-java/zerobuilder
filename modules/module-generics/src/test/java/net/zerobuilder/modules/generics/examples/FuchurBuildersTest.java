package net.zerobuilder.modules.generics.examples;

import org.junit.Test;

import java.util.Map;

import static java.util.Arrays.asList;
import static net.zerobuilder.modules.generics.examples.FuchurBuilders.multiKeyBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class FuchurBuildersTest {

  @Test
  public void multi() throws Exception {
    Map<String, Integer> m = multiKeyBuilder()
        .keys(asList("1", "2"))
        .value(2);
    assertThat(m.size(), is(2));
    assertThat(m.get("1"), is(2));
    assertThat(m.get("2"), is(2));
  }
}