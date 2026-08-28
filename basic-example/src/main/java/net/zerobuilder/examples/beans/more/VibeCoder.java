package net.zerobuilder.examples.beans.more;

import net.zerobuilder.RecordBuilder;
import net.zerobuilder.StepName;

import java.util.List;

@RecordBuilder
record VibeCoder(
    String name,
    int age,
    List<String> notes,
    @StepName("executive")
    boolean isExecutive) {
  String executive() {
    return null;
  }
}
