package net.zerobuilder.examples.beans.more;

import net.zerobuilder.Builder;
import net.zerobuilder.Name;
import net.zerobuilder.Updater;

import java.util.List;

public record VibeCoder(
    String name,
    int age,
    List<String> notes,
    @Name("executive") // TODO this should have an effect
    boolean isExecutive) {

  @Builder
  @Updater
  public VibeCoder {

  }
}
