package net.zerobuilder.examples.beans;

import org.junit.Test;

import static net.zerobuilder.examples.beans.ManagerBuilders.managerBuilder;
import static net.zerobuilder.examples.beans.ManagerBuilders.managerUpdater;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class ManagerTest {

  @Test
  public void basic() {
    Manager foo = managerBuilder()
        .name("foo")
        .id(12)
        .salary(13);
    Manager bar = managerUpdater(foo)
        .name("bar")
        .done();
    assertThat(foo.getSalary(), is(13));
    assertThat(foo.getName(), is("foo"));
    assertThat(foo.getId(), is(12));
    assertThat(bar.getSalary(), is(13));
    assertThat(bar.getName(), is("bar"));
    assertThat(bar.getId(), is(12));
  }

}