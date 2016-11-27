package net.zerobuilder.examples.values;

import net.zerobuilder.Builders;
import net.zerobuilder.Step;
import net.zerobuilder.Goal;

// changing step order
@Builders(recycle = true)
final class Spaghetti {

  final String cheese;
  final String sauce;
  final boolean alDente;

  @Goal(updater = true)
  Spaghetti(String cheese, @Step(0) String sauce, boolean alDente) {
    this.cheese = cheese;
    this.sauce = sauce;
    this.alDente = alDente;
  }

  static SpaghettiBuilders.SpaghettiBuilder
      .Cheese napoliBuilder() {
    return SpaghettiBuilders.spaghettiBuilder().sauce("tomato");
  }

}
