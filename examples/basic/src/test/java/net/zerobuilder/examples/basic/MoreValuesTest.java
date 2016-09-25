package net.zerobuilder.examples.basic;

import net.zerobuilder.examples.basic.MoreValues.Interface;
import org.junit.Test;

import static net.zerobuilder.examples.basic.MoreValues_InterfaceBuilders.interfaceBuilder;
import static net.zerobuilder.examples.basic.MoreValues_NothingBuilders.appendBuilder;
import static net.zerobuilder.examples.basic.MoreValues_SumBuilders.sumBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MoreValuesTest {

  @Test
  public void testDefault() {
    Interface foo = interfaceBuilder().foo("foo");
    assertThat(foo.foo, is("foo"));
  }

  @Test
  public void testNothing() {
    StringBuilder sb = new StringBuilder();
    appendBuilder().sb(sb).word("Hello");
    appendBuilder().sb(sb).word(", ");
    appendBuilder().sb(sb).word("World!");
    assertThat(sb.toString(), is("Hello, World!"));
  }

  @Test
  public void testSum() {
    int sum = sumBuilder().a(5).b(8);
    assertThat(sum, is(13));
  }
}