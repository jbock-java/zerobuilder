package net.zerobuilder.examples.beans;

import net.zerobuilder.BeanBuilder;

@BeanBuilder
class User {

  private int id;
  private String name;
  private boolean power;

  int getId() {
    return id;
  }

  String getName() {
    return name;
  }

  void setId(int id) {
    this.id = id;
  }

  void setName(String name) {
    this.name = name;
  }
  boolean isPower() {
    return power;
  }
  void setPower(boolean power) {
    this.power = power;
  }
}