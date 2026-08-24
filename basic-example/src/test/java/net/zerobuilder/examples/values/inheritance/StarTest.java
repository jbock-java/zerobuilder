package net.zerobuilder.examples.values.inheritance;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static net.zerobuilder.examples.values.inheritance.StarBuilders.builder;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StarTest {

  @Test
  void test() {
    Star sirius = builder()
        .mass(BigInteger.valueOf(202));
    Star rigel = builder(sirius)
        .mass(BigInteger.valueOf(2300))
        .build();
    assertEquals(BigInteger.valueOf(202), sirius.mass);
    assertEquals(BigInteger.valueOf(2300), rigel.mass);
  }
}
