package net.zerobuilder.modules.generics.examples;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static java.util.Collections.singletonList;
import static net.zerobuilder.modules.generics.examples.TrickyGenericsBuilders.getListBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TrickyGenericsBuildersTest {

  @Test
  public void tricky() {
    HashMap<String, List<Integer>> m = new HashMap<>();
    List<Integer> integers = getListBuilder()
        .source(m)
        .key("12")
        .defaultValue(11);
    assertEquals(singletonList(11), integers);
  }
}
