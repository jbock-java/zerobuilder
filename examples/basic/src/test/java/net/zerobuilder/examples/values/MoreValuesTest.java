package net.zerobuilder.examples.values;

import net.zerobuilder.examples.values.MoreValues.Interface;
import net.zerobuilder.examples.values.MoreValues.NothingSpecial;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;

import static net.zerobuilder.examples.values.MoreValues_InterfaceBuilders.interfaceBuilder;
import static net.zerobuilder.examples.values.MoreValues_NothingBuilders.appendBuilder;
import static net.zerobuilder.examples.values.MoreValues_NothingSpecialBuilders.nothingSpecialBuilder;
import static net.zerobuilder.examples.values.MoreValues_NothingSpecialBuilders.nothingSpecialUpdater;
import static net.zerobuilder.examples.values.MoreValues_SumBuilders.sumBuilder;
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

  @Test
  public void testNothingSpecial() throws IOException {
    NothingSpecial foo = nothingSpecialBuilder()
        .foo("foo");
    NothingSpecial bar = nothingSpecialUpdater(foo)
        .foo("bar")
        .done();
    assertThat(foo.foo(), is("foo"));
    assertThat(bar.foo(), is("bar"));
  }

  @Test
  public void testNoGoals() {
    Method[] declaredMethods = MoreValues_NoGoalsBuilders.class.getDeclaredMethods();
    assertThat(declaredMethods.length, is(0));
  }
}