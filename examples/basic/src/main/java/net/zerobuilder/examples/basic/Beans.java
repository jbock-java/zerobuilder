package net.zerobuilder.examples.basic;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;
import net.zerobuilder.examples.beans.Employee;
import net.zerobuilder.examples.beans.Manager;
import net.zerobuilder.examples.beans.User;

// see BeansTest
@Builders
@SuppressWarnings("unused")
final class Beans {

  @Goal(toBuilder = true)
  private static final User USER = null;

  @Goal
  private static final Employee EMPLOYEE = null;

  @Builders(recycle = true)
  @Goal(name = "calendar", toBuilder = true)
  public static final class MyCalendar {

    private String currentYear;
    private long unixTime;
    private boolean dst;

    public long getUnixTime() {
      return unixTime;
    }

    public void setUnixTime(long unixTime) {
      this.unixTime = unixTime;
    }

    public boolean isDst() {
      return dst;
    }

    public void setDst(boolean dst) {
      this.dst = dst;
    }

    public String getCurrentYear() {
      return currentYear;
    }

    public void setCurrentYear(String currentYear) {
      this.currentYear = currentYear;
    }
  }

  @Builders(recycle = true)
  final static class MoreBeans {

    @Goal(toBuilder = true)
    private static final Manager MANAGER = null;

    private MoreBeans() {
      throw new UnsupportedOperationException("no instances");
    }
  }

  private Beans() {
    throw new UnsupportedOperationException("no instances");
  }
}
