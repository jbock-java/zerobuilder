package net.zerobuilder.examples.beans.more;

import java.util.List;
import net.zerobuilder.StepName;
import net.zerobuilder.RecordBuilder;

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
