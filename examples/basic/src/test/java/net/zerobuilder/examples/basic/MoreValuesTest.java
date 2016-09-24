package net.zerobuilder.examples.basic;

import net.zerobuilder.examples.basic.MoreValues.Interface;
import org.junit.Test;

import static net.zerobuilder.examples.basic.MoreValues_InterfaceBuilders.interfaceBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class MoreValuesTest {

  @Test
  public void testDefault() {
    Interface foo = interfaceBuilder().foo("foo");
    assertThat(foo.foo, is("foo"));
  }
}