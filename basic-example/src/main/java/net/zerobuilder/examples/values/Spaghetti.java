package net.zerobuilder.examples.values;

import net.zerobuilder.RecordBuilder;
import net.zerobuilder.StepOrder;

// changing step order
@RecordBuilder
final class Spaghetti {

  final String cheese;
  final String sauce;
  final boolean alDente;

  Spaghetti(String cheese, @StepOrder(0) String sauce, boolean alDente) {
    this.cheese = cheese;
    this.sauce = sauce;
    this.alDente = alDente;
  }

  static SpaghettiBuilders.CheeseStep napoliBuilder() {
    return SpaghettiBuilders.builder().sauce("tomato");
  }
}
