package net.zerobuilder.examples.beans;

import net.zerobuilder.Step;

public class Employee {

  private int id;
  private int salary;
  private String name;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getSalary() {
    return salary;
  }

  public void setSalary(int salary) {
    this.salary = salary;
  }

  public String getName() {
    return name;
  }

  @Step(1)
  public void setName(String name) {
    this.name = name;
  }

}