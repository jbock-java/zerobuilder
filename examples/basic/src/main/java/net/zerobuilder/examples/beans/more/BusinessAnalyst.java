package net.zerobuilder.examples.beans.more;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.util.ArrayList;
import java.util.List;

@Builders
@Goal(toBuilder = true)
public class BusinessAnalyst {

  private String name;
  private int age;

  private List<String> strings;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  public List<String> getStrings() {
    if (strings == null) {
      strings = new ArrayList<>();
    }
    return strings;
  }
}
