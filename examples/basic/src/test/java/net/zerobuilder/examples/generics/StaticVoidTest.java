package net.zerobuilder.examples.generics;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static net.zerobuilder.examples.generics.StaticVoidBuilders.twinsBuilder;

public class StaticVoidTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void twins() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("twins");
    twinsBuilder().key("a").value("b");
  }

  @Test
  public void notTwins() {
    twinsBuilder().key("a").value(1);
  }

}