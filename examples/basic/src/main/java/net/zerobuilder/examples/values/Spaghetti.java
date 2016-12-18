package net.zerobuilder.examples.values;

import net.zerobuilder.Builder;
import net.zerobuilder.Recycle;
import net.zerobuilder.Step;
import net.zerobuilder.Style;
import net.zerobuilder.Updater;

// changing step order
final class Spaghetti {

  final String cheese;
  final String sauce;
  final boolean alDente;

  @Builder(style = Style.IMMUTABLE)
  @Updater
  @Recycle
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
