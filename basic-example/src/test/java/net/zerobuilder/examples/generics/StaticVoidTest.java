package net.zerobuilder.examples.generics;

import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.generics.StaticVoidBuilders.twinsBuilder;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StaticVoidTest {

  @Test
  public void twins() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> twinsBuilder().key("a").value("b"));
    assertTrue(ex.getMessage().contains("twins"));
  }

  @Test
  public void notTwins() {
    twinsBuilder().key("a").value(1);
  }

}
