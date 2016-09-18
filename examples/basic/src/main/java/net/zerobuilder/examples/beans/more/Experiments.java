package net.zerobuilder.examples.beans.more;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;

import java.util.ArrayList;
import java.util.List;

// see ExperimentsTest
public class Experiments {

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

  // setterless generic collection
  @Builders(recycle = true)
  @Goal(toBuilder = true)
  public static class BioExperiment {
    private List<List<String>> pigs;

    public List<List<String>> getPigs() {
      if (pigs == null) {
        pigs = new ArrayList<>();
      }
      return pigs;
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
}
