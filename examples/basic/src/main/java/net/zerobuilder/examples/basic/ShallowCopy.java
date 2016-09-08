package net.zerobuilder.examples.basic;

import net.zerobuilder.BeanGoal;
import net.zerobuilder.Build;
import net.zerobuilder.examples.beans.Employee;
import net.zerobuilder.examples.beans.User;

@Build
final class ShallowCopy {

  private ShallowCopy() {
  }

  @BeanGoal
  @SuppressWarnings("unused")
  static final User USER = null;

  @BeanGoal
  @SuppressWarnings("unused")
  static final Employee EMPLOYEE = null;

}
