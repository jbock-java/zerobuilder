package net.zerobuilder.examples.values;

import net.zerobuilder.examples.values.MoreValues.Interface;
import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.values.MoreValues_InterfaceBuilders.interfaceBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MoreValuesTest {

  @Test
  void testDefault() {
    Interface foo = interfaceBuilder().foo("foo");
    assertEquals("foo", foo.foo);
  }
}
