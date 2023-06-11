package net.zerobuilder.examples.beans;

import net.zerobuilder.examples.beans.UserBuilders.UserBuilder;
import net.zerobuilder.examples.beans.UserBuilders.UserUpdater;
import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.beans.UserBuilders.userBuilder;
import static net.zerobuilder.examples.beans.UserBuilders.userUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class UserTest {

  @Test
  public void basic() {
    UserBuilder.Id builder = userBuilder();
    User foo = builder
        .id(12)
        .name("foo")
        .power(false);
    UserUpdater updater = userUpdater(foo);
    User bar = updater
        .name("bar")
        .done();
    assertEquals(12, foo.getId());
    assertEquals("foo", foo.getName());
    assertFalse(foo.isPower());
    assertEquals(12, bar.getId());
    assertEquals("bar", bar.getName());
    assertFalse(bar.isPower());
  }
}
