package net.zerobuilder.examples.beans.more;

import net.zerobuilder.examples.beans.more.MoreBeans.AeroExperiment;
import net.zerobuilder.examples.beans.more.MoreBeans.BioExperiment;
import net.zerobuilder.examples.beans.more.MoreBeans.Ignorify;
import net.zerobuilder.examples.beans.more.MoreBeans.IterableExperiment;
import net.zerobuilder.examples.beans.more.MoreBeans.OverloadedExperiment;
import net.zerobuilder.examples.beans.more.MoreBeans.RawExperiment;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.zerobuilder.examples.beans.more.MoreBeans_AeroExperimentBuilders.aeroExperimentBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_AeroExperimentBuilders.aeroExperimentUpdater;
import static net.zerobuilder.examples.beans.more.MoreBeans_BioExperimentBuilders.bioExperimentBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_BioExperimentBuilders.bioExperimentUpdater;
import static net.zerobuilder.examples.beans.more.MoreBeans_IgnorifyBuilders.ignorifyBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_IgnorifyBuilders.ignorifyUpdater;
import static net.zerobuilder.examples.beans.more.MoreBeans_IterableExperimentBuilders.iterableExperimentBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_IterableExperimentBuilders.iterableExperimentUpdater;
import static net.zerobuilder.examples.beans.more.MoreBeans_OverloadedExperimentBuilders.overloadedExperimentBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_OverloadedExperimentBuilders.overloadedExperimentUpdater;
import static net.zerobuilder.examples.beans.more.MoreBeans_RawExperimentBuilders.rawExperimentBuilder;
import static net.zerobuilder.examples.beans.more.MoreBeans_RawExperimentBuilders.rawExperimentUpdater;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MoreBeansTest {

  @Test
  public void atmosphericTest() {
    AeroExperiment experiment1 = aeroExperimentBuilder()
        .altitude(10)
        .yield(20);
    AeroExperiment experiment2 = aeroExperimentUpdater(experiment1)
        .yield(100)
        .done();
    assertThat(experiment1.getAltitude(), is(10));
    assertThat(experiment1.getYield(), is(20));
    assertThat(experiment2.getAltitude(), is(10));
    assertThat(experiment2.getYield(), is(100));
  }

  @Test
  public void overloadedTest() {
    OverloadedExperiment experiment1 = overloadedExperimentBuilder()
        .yield(10);
    OverloadedExperiment experiment2 = overloadedExperimentUpdater(experiment1)
        .yield(20)
        .done();
    assertThat(experiment1.getYield(), is(10));
    assertThat(experiment2.getYield(), is(20));
  }

  @Test
  public void biologicalTest() {
    BioExperiment experiment1 = bioExperimentBuilder()
        .candidates(singletonList(asList("Goku", "Frieza")));
    BioExperiment experiment2 = bioExperimentUpdater(experiment1)
        .candidates(singletonList(asList("Gohan")))
        .done();
    assertThat(experiment1.getCandidates(), is(singletonList(asList("Goku", "Frieza"))));
    assertThat(experiment2.getCandidates(), is(singletonList(asList("Gohan"))));
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void objectTest() {
    RawExperiment experiment1 = rawExperimentBuilder()
        .things(asList(1, "one"));
    RawExperiment experiment2 = rawExperimentUpdater(experiment1)
        .things(asList(2))
        .done();
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
    IterableExperiment experiment1 = iterableExperimentBuilder()
        .things(asList(asList("1")));
    IterableExperiment experiment2 = iterableExperimentUpdater(experiment1)
        .things(asList(asList("2")))
        .done();
    List<Iterable<String>> expectedList1 = new ArrayList();
    expectedList1.add(asList("1"));
    List<Iterable<String>> expectedList2 = new ArrayList();
    expectedList2.add(asList("2"));
    assertThat(experiment1.getThings(), is(expectedList1));
    assertThat(experiment2.getThings(), is(expectedList2));
  }

  @Test
  public void ignoreTest() {
    Ignorify nothing = ignorifyBuilder().things(emptyList());
    Ignorify something = ignorifyUpdater(nothing)
        .things(singletonList(singletonList("a")))
        .done();
    Ignorify nothing2 = ignorifyUpdater(nothing)
        .things(emptyList())
        .done();
    assertThat(nothing.getThings().size(), is(0));
    assertThat(something.getThings().size(), is(1));
    assertThat(something.getThings().get(0),
        is(singletonList("a")));
    assertThat(nothing2.getThings().size(), is(0));
  }
}