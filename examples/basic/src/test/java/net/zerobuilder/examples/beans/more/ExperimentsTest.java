package net.zerobuilder.examples.beans.more;

import net.zerobuilder.examples.beans.more.Experiments.AeroExperiment;
import net.zerobuilder.examples.beans.more.Experiments.BioExperiment;
import net.zerobuilder.examples.beans.more.Experiments.IterableExperiment;
import net.zerobuilder.examples.beans.more.Experiments.RawExperiment;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static net.zerobuilder.examples.beans.more.Experiments_AeroExperimentBuilders.aeroExperimentBuilder;
import static net.zerobuilder.examples.beans.more.Experiments_AeroExperimentBuilders.aeroExperimentToBuilder;
import static net.zerobuilder.examples.beans.more.Experiments_BioExperimentBuilders.bioExperimentBuilder;
import static net.zerobuilder.examples.beans.more.Experiments_BioExperimentBuilders.bioExperimentToBuilder;
import static net.zerobuilder.examples.beans.more.Experiments_RawExperimentBuilders.rawExperimentBuilder;
import static net.zerobuilder.examples.beans.more.Experiments_RawExperimentBuilders.rawExperimentToBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExperimentsTest {

  @Test
  public void atmosphericTest() {
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

  @Test
  public void biologicalTest() {
    BioExperiment experiment1 = bioExperimentBuilder()
        .pigs(asList(asList("Rosie", "Donna")));
    BioExperiment experiment2 = bioExperimentToBuilder(experiment1)
        .pigs(asList("Daisy"))
        .build();
    assertThat(experiment1.getPigs(), is(asList(asList("Rosie", "Donna"))));
    assertThat(experiment2.getPigs(), is(asList(asList("Daisy"))));
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void objectTest() {
    RawExperiment experiment1 = rawExperimentBuilder()
        .things(asList(1, "one"));
    RawExperiment experiment2 = rawExperimentToBuilder(experiment1)
        .things(asList(2))
        .build();
    List expectedList1 = new ArrayList();
    expectedList1.add(1);
    expectedList1.add("one");
    List expectedList2 = new ArrayList();
    expectedList2.add(2);
    assertThat(experiment1.getThings(), is(expectedList1));
    assertThat(experiment2.getThings(), is(expectedList2));
  }

  @Test
  public void iterableTest() {
    IterableExperiment experiment1 = Experiments_IterableExperimentBuilders.iterableExperimentBuilder()
        .things(asList(asList("1")));
    IterableExperiment experiment2 = Experiments_IterableExperimentBuilders.iterableExperimentToBuilder(experiment1)
        .things(asList(asList("2")))
        .build();
    List<Iterable<String>> expectedList1 = new ArrayList();
    expectedList1.add(asList("1"));
    List<Iterable<String>> expectedList2 = new ArrayList();
    expectedList2.add(asList("2"));
    assertThat(experiment1.getThings(), is(expectedList1));
    assertThat(experiment2.getThings(), is(expectedList2));
  }
}