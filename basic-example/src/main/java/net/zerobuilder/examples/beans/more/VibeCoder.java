package net.zerobuilder.examples.beans.more;

import java.util.List;
import net.zerobuilder.Builder;
import net.zerobuilder.Updater;

public record VibeCoder(
    String name,
    int age,
    List<String> notes,
    boolean isExecutive) {

  @Builder
  @Updater
  public VibeCoder {

  }
}
