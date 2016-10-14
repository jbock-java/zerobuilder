package net.zerobuilder.examples.beans;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.util.List;

// inheritance + overrides
@Builders
@Goal(updater = true)
final class Manager extends Employee {

  private Manager boss;

  @Override
  int getSalary() {
    return super.getSalary();
  }

  @Override
  void setSalary(int salary) {
    super.setSalary(salary);
  }

  public Manager getBoss() {
    return boss;
  }
  public void setBoss(Manager boss) {
    this.boss = boss;
  }
}