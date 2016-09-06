package net.zerobuilder.examples.basic;

import net.zerobuilder.Build;
import net.zerobuilder.FloatLeft;
import net.zerobuilder.Goal;

@Build(recycle = true)
final class Spaghetti {

  final String cheese;
  final String sauce;
  final boolean alDente;

  @Goal(toBuilder = true)
  Spaghetti(String cheese, @FloatLeft String sauce, boolean alDente) {
    this.cheese = cheese;
    this.sauce = sauce;
    this.alDente = alDente;
  }

  static SpaghettiBuilders.SpaghettiBuilder.Contract.Cheese napoliBuilder() {
    return SpaghettiBuilders.spaghettiBuilder().sauce("tomato");
  }

}
