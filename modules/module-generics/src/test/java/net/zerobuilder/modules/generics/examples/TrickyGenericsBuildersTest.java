package net.zerobuilder.modules.generics.examples;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static java.util.Collections.singletonList;
import static net.zerobuilder.modules.generics.examples.TrickyGenericsBuilders.getListBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TrickyGenericsBuildersTest {

  @Test
  public void tricky() {
    HashMap<String, List<Integer>> m = new HashMap<>();
    List<Integer> integers = getListBuilder()
        .source(m)
        .key("12")
        .defaultValue(11);
    assertThat(integers, is(singletonList(11)));
  }
}
