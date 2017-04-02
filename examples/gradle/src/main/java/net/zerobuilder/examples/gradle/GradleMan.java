package net.zerobuilder.examples.gradle;

import net.zerobuilder.Builder;

public final class GradleMan {

  final String message;

  @Builder
  GradleMan (String message) {
    this.message = message;
  }

  public static void main(String[] args) {
    GradleMan gradleMan = GradleManBuilders.gradleManBuilder().message("Hello gradle!");
    System.out.println(gradleMan.message);
  }
}
