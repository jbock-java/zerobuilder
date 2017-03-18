package net.zerobuilder.examples.javac;

import net.zerobuilder.Builder;

public final class JavaMan {

  final String message;

  @Builder
  JavaMan (String message) {
    this.message = message;
  }

  public static void main(String[] args) {
    JavaMan javaMan = JavaManBuilders.javaManBuilder().message("Hello world!");
    System.out.println(javaMan.message);
  }
}
