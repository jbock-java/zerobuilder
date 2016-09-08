package net.zerobuilder.examples.basic;

import net.zerobuilder.Builder;
import net.zerobuilder.Step;
import net.zerobuilder.Goal;

@Builder(recycle = true)
final class Spaghetti {

  final String cheese;
  final String sauce;
  final boolean alDente;

  @Goal(toBuilder = true)
  Spaghetti(String cheese, @Step(0) String sauce, boolean alDente) {
    this.cheese = cheese;
    this.sauce = sauce;
    this.alDente = alDente;
  }

  static SpaghettiBuilders.SpaghettiBuilder.Contract.Cheese napoliBuilder() {
    return SpaghettiBuilders.spaghettiBuilder().sauce("tomato");
  }

}
