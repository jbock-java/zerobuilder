package net.zerobuilder.examples.values.inheritance;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.values.inheritance.StarBuilders.starBuilder;
import static net.zerobuilder.examples.values.inheritance.StarBuilders.starUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StarTest {

  @Test
  void test() {
    Star sirius = starBuilder()
        .mass(BigInteger.valueOf(202));
    Star rigel = starUpdater(sirius)
        .mass(BigInteger.valueOf(2300))
        .build();
    assertEquals(BigInteger.valueOf(202), sirius.mass);
    assertEquals(BigInteger.valueOf(2300), rigel.mass);
  }
}
