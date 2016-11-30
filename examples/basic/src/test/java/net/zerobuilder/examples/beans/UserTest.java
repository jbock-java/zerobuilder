package net.zerobuilder.examples.beans;

import net.zerobuilder.examples.beans.UserBuilders.UserBuilder;
import net.zerobuilder.examples.beans.UserBuilders.UserUpdater;
import org.junit.Test;

import static net.zerobuilder.examples.beans.UserBuilders.userBuilder;
import static net.zerobuilder.examples.beans.UserBuilders.userUpdater;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class UserTest {

  @Test
  public void basic() throws Exception {
    UserBuilder.Id builder = userBuilder();
    User foo = builder
        .id(12)
        .name("foo")
        .power(false);
    UserUpdater updater = userUpdater(foo);
    User bar = updater
        .name("bar")
        .done();
    assertThat(foo.getId(), is(12));
    assertThat(foo.getName(), is("foo"));
    assertThat(foo.isPower(), is(false));
    assertThat(bar.getId(), is(12));
    assertThat(bar.getName(), is("bar"));
    assertThat(bar.isPower(), is(false));
    // check that caching works
    assertTrue(userBuilder() == builder);
    assertTrue(userUpdater(foo) == updater);
  }
}