package net.zerobuilder.examples.beans;

import org.junit.Test;

import static net.zerobuilder.examples.beans.ManagerBuilders.managerBuilder;
import static net.zerobuilder.examples.beans.ManagerBuilders.managerUpdater;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class ManagerTest {

  @Test
  public void basic() {
    Manager foo = managerBuilder()
        .name("foo")
        .boss(managerBuilder()
            .name("123")
            .boss(null)
            .id(4)
            .salary(192873))
        .id(12)
        .salary(13);
    Manager bar = managerUpdater(foo)
        .name("bar")
        .boss(null)
        .done();
    assertThat(foo.getSalary(), is(13));
    assertThat(foo.getName(), is("foo"));
    assertThat(foo.getId(), is(12));
    assertThat(foo.getBoss().getName(), is("123"));
    assertThat(foo.getBoss().getBoss(), is(nullValue()));
    assertThat(foo.getBoss().getId(), is(4));
    assertThat(foo.getBoss().getSalary(), is(192873));
    assertThat(bar.getSalary(), is(13));
    assertThat(bar.getName(), is("bar"));
    assertThat(bar.getBoss(), is(nullValue()));
    assertThat(bar.getId(), is(12));
  }

  @Test(expected = NullPointerException.class)
  public void nullBuilder() {
    Manager foo = managerBuilder()
        .name(null)
        .boss(null)
        .id(12)
        .salary(13);
  }

  @Test(expected = NullPointerException.class)
  public void nullUpdater() {
    Manager foo = managerBuilder()
        .name("foo")
        .boss(managerBuilder()
            .name("123")
            .boss(null)
            .id(4)
            .salary(192873))
        .id(12)
        .salary(13);
    Manager bar = managerUpdater(foo)
        .name(null)
        .boss(null)
        .done();
  }
}