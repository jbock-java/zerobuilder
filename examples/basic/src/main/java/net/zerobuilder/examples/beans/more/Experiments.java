package net.zerobuilder.examples.beans.more;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;
import net.zerobuilder.Step;

import java.util.ArrayList;
import java.util.List;

// beans + inheritance
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
}
