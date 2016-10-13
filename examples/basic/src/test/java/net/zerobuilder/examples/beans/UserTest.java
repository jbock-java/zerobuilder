package net.zerobuilder.examples.beans;

import org.junit.Test;

import static net.zerobuilder.examples.beans.UserBuilders.userBuilder;
import static net.zerobuilder.examples.beans.UserBuilders.userUpdater;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class UserTest {

  @Test
  public void basic() throws Exception {
    User foo = userBuilder()
        .id(12)
        .name("foo")
        .power(false);
    User bar = userUpdater(foo)
        .name("bar")
        .done();
    assertThat(foo.getId(), is(12));
    assertThat(foo.getName(), is("foo"));
    assertThat(foo.isPower(), is(false));
    assertThat(bar.getId(), is(12));
    assertThat(bar.getName(), is("bar"));
    assertThat(bar.isPower(), is(false));
  }
}