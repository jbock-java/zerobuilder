package net.zerobuilder.examples.beans.string;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.util.ArrayList;
import java.util.List;

// danger of variable name conflict
@Builders
@Goal(toBuilder = true, nonNull = true)
public class String {

  private List<String> string;
  public List<String> getString() {
    if (string == null) {
      string = new ArrayList<>();
    }
    return string;
  }
}
