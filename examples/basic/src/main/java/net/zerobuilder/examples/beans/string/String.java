package net.zerobuilder.examples.beans.string;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.util.ArrayList;
import java.util.List;

import static net.zerobuilder.NullPolicy.REJECT;

// danger of variable name conflict
@Builders
@Goal(toBuilder = true, nullPolicy = REJECT)
public class String {

  private List<String> string;
  public List<String> getString() {
    if (string == null) {
      string = new ArrayList<>();
    }
    return string;
  }
}
