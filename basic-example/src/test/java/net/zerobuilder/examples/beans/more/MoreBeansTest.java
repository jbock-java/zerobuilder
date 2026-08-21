package net.zerobuilder.examples.beans.more;

import net.zerobuilder.examples.beans.more.MoreBeans.AeroExperiment;
import net.zerobuilder.examples.beans.more.MoreBeans.BioExperiment;
import net.zerobuilder.examples.beans.more.MoreBeans.Ignorify;
import net.zerobuilder.examples.beans.more.MoreBeans.IterableExperiment;
import net.zerobuilder.examples.beans.more.MoreBeans.OverloadedExperiment;
import net.zerobuilder.examples.beans.more.MoreBeans.RawExperiment;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MoreBeansTest {

  @Test
  public void atmosphericTest() {
    AeroExperiment experiment1 = aeroExperimentBuilder()
        .altitude(10)
        .yield(20);
    AeroExperiment experiment2 = aeroExperimentUpdater(experiment1)
        .yield(100)
        .done();
    assertEquals(10, experiment1.getAltitude());
    assertEquals(20, experiment1.getYield());
    assertEquals(10, experiment2.getAltitude());
    assertEquals(100, experiment2.getYield());
  }

  @Test
  public void overloadedTest() {
    OverloadedExperiment experiment1 = overloadedExperimentBuilder()
        .yield(10);
    OverloadedExperiment experiment2 = overloadedExperimentUpdater(experiment1)
        .yield(20)
        .done();
    assertEquals(10, experiment1.getYield());
    assertEquals(20, experiment2.getYield());
  }

  @Test
  public void biologicalTest() {
    BioExperiment experiment1 = bioExperimentBuilder()
        .candidates(List.of(List.of("Goku", "Frieza")));
    BioExperiment experiment2 = bioExperimentUpdater(experiment1)
        .candidates(List.of(List.of("Gohan")))
        .done();
    assertEquals(List.of(List.of("Goku", "Frieza")), experiment1.getCandidates());
    assertEquals(List.of(List.of("Gohan")), experiment2.getCandidates());
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void objectTest() {
    RawExperiment experiment1 = rawExperimentBuilder()
        .things(List.of(1, "one"));
    RawExperiment experiment2 = rawExperimentUpdater(experiment1)
        .things(List.of(2))
        .done();
    List expectedList1 = new ArrayList();
    expectedList1.add(1);
    expectedList1.add("one");
    List expectedList2 = new ArrayList();
    expectedList2.add(2);
    assertEquals(expectedList1, experiment1.getThings());
    assertEquals(expectedList2, experiment2.getThings());
  }

  @Test
  public void iterableTest() {
    IterableExperiment experiment1 = iterableExperimentBuilder()
        .things(List.of(List.of("1")));
    IterableExperiment experiment2 = iterableExperimentUpdater(experiment1)
        .things(List.of(List.of("2")))
        .done();
    List<Iterable<String>> expectedList1 = new ArrayList();
    expectedList1.add(List.of("1"));
    List<Iterable<String>> expectedList2 = new ArrayList();
    expectedList2.add(List.of("2"));
    assertEquals(expectedList1, experiment1.getThings());
    assertEquals(expectedList2, experiment2.getThings());
  }

  @Test
  public void ignoreTest() {
    Ignorify nothing = ignorifyBuilder().things(List.of());
    Ignorify something = ignorifyUpdater(nothing)
        .things(singletonList(singletonList("a")))
        .done();
    Ignorify nothing2 = ignorifyUpdater(nothing)
        .things(List.of())
        .done();
    assertEquals(0, nothing.getThings().size());
    assertEquals(1, something.getThings().size());
    assertEquals(singletonList("a"),
        something.getThings().get(0));
    assertEquals(0, nothing2.getThings().size());
  }
}
