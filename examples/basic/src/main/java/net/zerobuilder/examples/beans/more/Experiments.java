package net.zerobuilder.examples.beans.more;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;
import net.zerobuilder.Ignore;

import java.util.ArrayList;
import java.util.List;

// various beans
// see ExperimentsTest
public class Experiments {

  // standard bean
  @Builders(recycle = true)
  @Goal(toBuilder = true)
  public static class Experiment {
    private int yield;
    public int getYield() {
      return yield;
    }
    public void setYield(int yield) {
      this.yield = yield;
    }
  }

  // inheritance
  @Builders(recycle = true)
  @Goal(toBuilder = true)
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
  @Goal(toBuilder = true)
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
  @Goal(toBuilder = true)
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
  @Goal(toBuilder = true)
  public static class IterableExperiment {
    private List<Iterable<String>> things;

    public List<Iterable<String>> getThings() {
      if (things == null) {
        things = new ArrayList();
      }
      return things;
    }
  }

  // ignore annotation
  @Builders
  @Goal(toBuilder = true)
  public static class Ignorify {
    private List<Iterable<String>> things;
    @Ignore
    public String getSocks() {
      return "socks";
    }
    public List<Iterable<String>> getThings() {
      if (things == null) {
        things = new ArrayList();
      }
      return things;
    }
  }
}
