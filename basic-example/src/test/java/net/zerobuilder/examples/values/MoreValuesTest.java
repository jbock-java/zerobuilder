package net.zerobuilder.examples.values;

import java.io.IOException;
import net.zerobuilder.examples.values.MoreValues.Interface;
import net.zerobuilder.examples.values.MoreValues.NothingSpecial;
import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.values.MoreValues_InterfaceBuilders.interfaceBuilder;
import static net.zerobuilder.examples.values.MoreValues_NothingBuilders.appendBuilder;
import static net.zerobuilder.examples.values.MoreValues_NothingSpecialBuilders.nothingSpecialBuilder;
import static net.zerobuilder.examples.values.MoreValues_NothingSpecialBuilders.nothingSpecialUpdater;
import static net.zerobuilder.examples.values.MoreValues_SumBuilders.sumBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MoreValuesTest {

  @Test
  void testDefault() {
    Interface foo = interfaceBuilder().foo("foo");
    assertEquals("foo", foo.foo);
  }

  @Test
  void testNothing() {
    StringBuilder sb = new StringBuilder();
    appendBuilder().sb(sb).word("Hello");
    appendBuilder().sb(sb).word(", ");
    appendBuilder().sb(sb).word("World!");
    assertEquals("Hello, World!", sb.toString());
  }

  @Test
  void testSum() {
    int sum = sumBuilder().a(5).b(8);
    assertEquals(13, sum);
  }

  @Test
  void testNothingSpecial() throws IOException {
    NothingSpecial foo = nothingSpecialBuilder()
        .foo("foo");
    NothingSpecial bar = nothingSpecialUpdater(foo)
        .foo("bar")
        .build();
    assertEquals("foo", foo.foo());
    assertEquals("bar", bar.foo());
  }
}
