package net.zerobuilder.examples.values.inheritance;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

import static java.math.BigInteger.TEN;
import static net.zerobuilder.examples.values.inheritance.PlanetBuilders.builder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanetTest {

  @Test
  void buildPlanet() {
    BigInteger mass = new BigInteger("597237000000000000000000");
    Planet planet = builder()
        .mass(mass)
        .numberOfMoons(1)
        .habitable(true);
    planet = builder(planet).mass(mass.multiply(TEN)).build();
    assertEquals(1, planet.numberOfMoons());
    assertTrue(planet.habitable());
    assertEquals(mass.multiply(TEN), planet.getMass());
  }
}
