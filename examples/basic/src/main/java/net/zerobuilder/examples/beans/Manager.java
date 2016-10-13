package net.zerobuilder.examples.beans;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

// inheritance + overrides
@Builders
@Goal(updater = true)
final class Manager extends Employee {

  @Override
  int getSalary() {
    return super.getSalary();
  }

  @Override
  void setSalary(int salary) {
    super.setSalary(salary);
  }
}