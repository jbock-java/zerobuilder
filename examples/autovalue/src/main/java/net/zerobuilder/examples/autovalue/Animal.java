package net.zerobuilder.examples.autovalue;

import com.google.auto.value.AutoValue;
import net.zerobuilder.Builder;
import net.zerobuilder.Updater;

@AutoValue
abstract class Animal {

  @Builder
  @Updater
  static Animal create(String name, int numberOfLegs) {
    return new AutoValue_Animal(name, numberOfLegs);
  }

  static AnimalBuilders.AnimalBuilder.Name builder() {
    return AnimalBuilders.animalBuilder();
  }

  AnimalBuilders.AnimalUpdater updater() {
    return AnimalBuilders.animalUpdater(this);
  }

  abstract String name();
  abstract int numberOfLegs();
}