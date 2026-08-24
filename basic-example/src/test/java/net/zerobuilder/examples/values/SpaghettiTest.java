package net.zerobuilder.examples.values;

import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.values.Spaghetti.napoliBuilder;
import static net.zerobuilder.examples.values.SpaghettiBuilders.builder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaghettiTest {

  @Test
  void testSpaghettiBuilder() {
    Spaghetti spaghetti = napoliBuilder()
        .cheese("reggiano")
        .alDente(true);
    assertTrue(spaghetti.alDente);
    assertEquals("reggiano", spaghetti.cheese);
    assertEquals("tomato", spaghetti.sauce);
    spaghetti = builder(spaghetti)
        .sauce("hot salsa")
        .cheese("cheddar")
        .alDente(false)
        .build();
    assertFalse(spaghetti.alDente);
    assertEquals("cheddar", spaghetti.cheese);
    assertEquals("hot salsa", spaghetti.sauce);
  }
}
