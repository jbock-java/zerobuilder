package net.zerobuilder.examples.basic;

import net.zerobuilder.Builder;
import net.zerobuilder.Goal;
import net.zerobuilder.examples.beans.Accountant;
import net.zerobuilder.examples.beans.Employee;
import net.zerobuilder.examples.beans.Manager;
import net.zerobuilder.examples.beans.User;

// see BeansTest
@Builder
@SuppressWarnings("unused")
final class Beans {

  @Goal(toBuilder = true)
  private static final User USER = null;

  @Goal
  private static final Employee EMPLOYEE = null;

  @Builder(recycle = true)
  final static class MoreBeans {

    @Goal(toBuilder = true)
    private static final Manager MANAGER = null;

    @Goal
    private static final Accountant ACCOUNTANT = null;

    @Goal(name = "otherAccountant", toBuilder = true)
    private static final net.zerobuilder.examples.beans.more.Accountant OTHER_ACCOUNTANT = null;

    private MoreBeans() {
      throw new UnsupportedOperationException("no instances");
    }
  }

  private Beans() {
    throw new UnsupportedOperationException("no instances");
  }
}
