package net.zerobuilder.examples.beans.more;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;
import net.zerobuilder.Ignore;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static net.zerobuilder.AccessLevel.PACKAGE;
import static net.zerobuilder.AccessLevel.PUBLIC;

// various beans
public class MoreBeans {

  // standard bean
  @Builders(recycle = true)
  @Goal(updater = true)
  public static class Experiment {
    private int yield;
    public int getYield() {
      return yield;
    }
    public void setYield(int yield) {
      this.yield = yield;
    }
  }

  // overloaded setter
  @Builders(recycle = true)
  @Goal(updater = true)
  public static class OverloadedExperiment {
    private int yield;
    public int getYield() {
      return yield;
    }
    public void setYield(int yield) {
      this.yield = yield;
    }
    public void setYield(String yield) {
      this.yield = Integer.parseInt(yield);
    }
  }

  // inheritance
  @Builders(recycle = true)
  @Goal(updater = true)
  public static class AeroExperiment extends Experiment {
    private int altitude;
    public int getAltitude() {
      return altitude;
    }
    public void setAltitude(int altitude) {
      this.altitude = altitude;
    }
  }

  // setterless collection with complicated type
  @Builders(recycle = true)
  @Goal(updater = true)
  public static class BioExperiment {
    private List<List<String>> candidates;

    public List<List<String>> getCandidates() {
      if (candidates == null) {
        candidates = new ArrayList<>();
      }
      return candidates;
    }
  }

  // setterless raw collection
  @Builders(recycle = true)
  @Goal(updater = true)
  @SuppressWarnings("rawtypes")
  public static class RawExperiment {
    private List things;

    public List getThings() {
      if (things == null) {
        things = new ArrayList();
      }
      return things;
    }
  }

  // corner case: list<iterable>; must avoid "same erasure" error
  @Builders(recycle = true)
  @Goal(updater = true)
  public static class IterableExperiment {
    private List<Iterable<String>> things;

    public List<Iterable<String>> getThings() {
      if (things == null) {
        things = new ArrayList();
      }
      return things;
    }
  }

  // ignore an invalid getter
  @Goal(updater = true)
  public static class Ignorify {
    private List<Iterable<String>> things;
    @Ignore
    public String getSocks() throws IOException {
      return "socks";
    }
    public List<Iterable<String>> getThings() {
      if (things == null) {
        things = new ArrayList();
      }
      return things;
    }
  }

  // access rules: default is PACKAGE, but updater is PUBLIC
  @Builders(access = PACKAGE)
  @Goal(updater = true, updaterAccess = PUBLIC)
  public static class Access {
    private String foo;
    public String getFoo() {
      return foo;
    }
    public void setFoo(String foo) {
      this.foo = foo;
    }
  }

  // accessors declare exceptions
  @Goal(updater = true)
  public static class Kaboom {
    private String foo;
    private List<String> bar;
    private List<String> doo;

    Kaboom() throws SQLException {
    }

    public String getFoo() throws IOException {
      return foo;
    }

    public void setFoo(String foo) throws ClassNotFoundException {
      this.foo = foo;
    }

    public List<String> getBar() throws IOException {
      if (bar == null) {
        bar = new ArrayList<>();
      }
      return bar;
    }

    public List<String> getDoo() throws ClassNotFoundException {
      return doo;
    }

    public void setDoo(List<String> doo) throws IOException {
      this.doo = doo;
    }
  }

}
