package net.zerobuilder.examples.inheritance;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PlanetTest {

  @Test
  public void buildPlanet() throws Exception {
    Planet planet = PlanetBuilder.builder().mass(12).numberOfMoons(1).habitable(true);
    planet = PlanetBuilder.toBuilder(planet).mass(120).build();
    assertThat(planet.numberOfMoons, is(1));
    assertThat(planet.getMass(), is(120l));
  }

}