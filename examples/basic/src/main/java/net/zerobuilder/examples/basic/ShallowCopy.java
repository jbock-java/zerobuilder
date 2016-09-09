package net.zerobuilder.examples.basic;

import net.zerobuilder.Builder;
import net.zerobuilder.examples.beans.Employee;
import net.zerobuilder.examples.beans.User;

@Builder
final class ShallowCopy {

  private ShallowCopy() {
  }

//  @Goal(toBuilder = true)
  @SuppressWarnings("unused")
  static final User USER = null;

//  @Goal(toBuilder = true)
  @SuppressWarnings("unused")
  static final Employee EMPLOYEE = null;

}
