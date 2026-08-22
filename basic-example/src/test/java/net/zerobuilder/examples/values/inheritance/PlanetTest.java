package net.zerobuilder.examples.values.inheritance;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

import static java.math.BigInteger.TEN;
import static net.zerobuilder.examples.values.inheritance.PlanetBuilders.planetBuilder;
import static net.zerobuilder.examples.values.inheritance.PlanetBuilders.planetUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanetTest {

  @Test
  void buildPlanet() {
    BigInteger mass = new BigInteger("597237000000000000000000");
    Planet planet = planetBuilder()
        .mass(mass)
        .numberOfMoons(1)
        .habitable(true);
    planet = planetUpdater(planet).mass(mass.multiply(TEN)).build();
    assertEquals(1, planet.getNumberOfMoons());
    assertTrue(planet.isHabitable());
    assertEquals(mass.multiply(TEN), planet.getMass());
  }
}
