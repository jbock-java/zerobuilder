package net.zerobuilder.examples.beans;

public class User {

  private int id;
  private String name;
  private boolean power;

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setId(int id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }
  public boolean isPower() {
    return power;
  }
  public void setPower(boolean power) {
    this.power = power;
  }
}