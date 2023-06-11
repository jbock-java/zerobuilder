package net.zerobuilder.examples.values.inheritance;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static net.zerobuilder.examples.values.inheritance.StarBuilders.starBuilder;
import static net.zerobuilder.examples.values.inheritance.StarBuilders.starUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StarTest {

  @Test
  public void test() {
    Star sirius = starBuilder()
        .mass(BigInteger.valueOf(202));
    Star rigel = starUpdater(sirius)
        .mass(BigInteger.valueOf(2300))
        .done();
    assertEquals(sirius.mass, BigInteger.valueOf(202));
    assertEquals(rigel.mass, BigInteger.valueOf(2300));
  }
}
