package net.zerobuilder.examples.beans.more;

import net.zerobuilder.examples.beans.more.Experiments.AeroExperiment;
import org.junit.Test;

import static net.zerobuilder.examples.beans.more.Experiments_AeroExperimentBuilders.aeroExperimentBuilder;
import static net.zerobuilder.examples.beans.more.Experiments_AeroExperimentBuilders.aeroExperimentToBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class ExperimentsTest {

  @Test
  public void test() {
    AeroExperiment experiment1 = aeroExperimentBuilder()
        .altitude(10)
        .yield(20);
    AeroExperiment experiment2 = aeroExperimentToBuilder(experiment1)
        .yield(100)
        .build();
    assertThat(experiment1.getAltitude(), is(10));
    assertThat(experiment1.getYield(), is(20));
    assertThat(experiment2.getAltitude(), is(10));
    assertThat(experiment2.getYield(), is(100));
  }

}