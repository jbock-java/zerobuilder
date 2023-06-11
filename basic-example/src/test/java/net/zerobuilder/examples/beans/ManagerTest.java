package net.zerobuilder.examples.beans;

import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.beans.ManagerBuilders.managerBuilder;
import static net.zerobuilder.examples.beans.ManagerBuilders.managerUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    assertEquals(13, foo.getSalary());
    assertEquals("foo", foo.getName());
    assertEquals(12, foo.getId());
    assertEquals("123", foo.getBoss().getName());
    assertNull(foo.getBoss().getBoss());
    assertEquals(4, foo.getBoss().getId());
    assertEquals(192873, foo.getBoss().getSalary());
    assertEquals(13, bar.getSalary());
    assertEquals("bar", bar.getName());
    assertNull(bar.getBoss());
    assertEquals(12, bar.getId());
  }
}
