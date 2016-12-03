package net.zerobuilder.examples.beans.more;

import net.zerobuilder.BeanBuilder;
import net.zerobuilder.BeanIgnore;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// various beans
public class MoreBeans {

  // standard bean
  @BeanBuilder
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
  @BeanBuilder
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
  @BeanBuilder
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
  @BeanBuilder
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
  @BeanBuilder
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
  @BeanBuilder
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
  @BeanBuilder
  public static class Ignorify {
    private List<Iterable<String>> things;
    @BeanIgnore
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

  // accessors declare exceptions
  @BeanBuilder
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
