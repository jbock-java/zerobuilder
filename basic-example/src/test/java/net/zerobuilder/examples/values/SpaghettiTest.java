package net.zerobuilder.examples.values;

import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.values.SpaghettiBuilders.spaghettiUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpaghettiTest {

  @Test
  public void napoliBuilder() {
    Spaghetti spaghetti = Spaghetti.napoliBuilder()
        .cheese("reggiano")
        .alDente(true);
    assertTrue(spaghetti.alDente);
    assertEquals(spaghetti.cheese, "reggiano");
    assertEquals(spaghetti.sauce, "tomato");
    spaghetti = spaghettiUpdater(spaghetti)
        .sauce("hot salsa")
        .cheese("cheddar")
        .alDente(false).done();
    assertFalse(spaghetti.alDente);
    assertEquals("cheddar", spaghetti.cheese);
    assertEquals("hot salsa", spaghetti.sauce);
  }

}
