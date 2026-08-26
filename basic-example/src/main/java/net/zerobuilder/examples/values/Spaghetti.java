package net.zerobuilder.examples.values;

import net.zerobuilder.Builder;
import net.zerobuilder.StepOrder;

// changing step order
final class Spaghetti {

  final String cheese;
  final String sauce;
  final boolean alDente;

  @Builder
  Spaghetti(String cheese, @StepOrder(0) String sauce, boolean alDente) {
    this.cheese = cheese;
    this.sauce = sauce;
    this.alDente = alDente;
  }

  static SpaghettiBuilders.CheeseStep napoliBuilder() {
    return SpaghettiBuilders.builder().sauce("tomato");
  }
}
