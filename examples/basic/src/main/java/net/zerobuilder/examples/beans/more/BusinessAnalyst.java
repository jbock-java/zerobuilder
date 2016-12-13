package net.zerobuilder.examples.beans.more;

import net.zerobuilder.BeanBuilder;
import net.zerobuilder.BeanRejectNull;

import java.util.ArrayList;
import java.util.List;

// bean with setterless collection (notes)
// see BusinessAnalystTest
@BeanBuilder
public class BusinessAnalyst {

  private String name;
  private int age;
  private List<String> notes;
  private boolean isExecutive;

  @BeanRejectNull
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

  public List<String> getNotes() {
    if (notes == null) {
      notes = new ArrayList<>();
    }
    return notes;
  }

  public boolean isExecutive() {
    return isExecutive;
  }
  public void setExecutive(boolean executive) {
    isExecutive = executive;
  }
}
