package net.zerobuilder.examples.beans.string;


import net.zerobuilder.BeanBuilder;

import java.util.ArrayList;
import java.util.List;

// danger of variable name conflict
@BeanBuilder
public class String {

  private List<String> string;
  public List<String> getString() {
    if (string == null) {
      string = new ArrayList<>();
    }
    return string;
  }
}
