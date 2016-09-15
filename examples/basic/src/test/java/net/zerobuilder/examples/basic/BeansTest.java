package net.zerobuilder.examples.basic;

import net.zerobuilder.examples.basic.Beans.MyCalendar;
import net.zerobuilder.examples.beans.Employee;
import net.zerobuilder.examples.beans.Manager;
import net.zerobuilder.examples.beans.User;
import org.junit.Test;

import static net.zerobuilder.examples.basic.BeansBuilders.employeeBuilder;
import static net.zerobuilder.examples.basic.BeansBuilders.userBuilder;
import static net.zerobuilder.examples.basic.BeansBuilders.userToBuilder;
import static net.zerobuilder.examples.basic.Beans_MoreBeansBuilders.managerBuilder;
import static net.zerobuilder.examples.basic.Beans_MoreBeansBuilders.managerToBuilder;
import static net.zerobuilder.examples.basic.Beans_MyCalendarBuilders.calendarBuilder;
import static net.zerobuilder.examples.basic.Beans_MyCalendarBuilders.calendarToBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BeansTest {

  @Test
  public void testEmployee() {
    Employee employee = employeeBuilder()
        .id(4711)
        .salary(60 * 1000)
        .name("Herbert");
    assertThat(employee.getId(), is(4711));
    assertThat(employee.getName(), is("Herbert"));
    assertThat(employee.getSalary(), is(60 * 1000));
  }

  @Test
  public void testUser() {
    User ben = userBuilder()
        .id(10)
        .name("Ben")
        .power(false);
    User rosie = userToBuilder(ben)
        .name("Rosie")
        .power(true)
        .build();
    assertThat(ben.getId(), is(10));
    assertThat(ben.getName(), is("Ben"));
    assertThat(ben.isPower(), is(false));
    assertThat(rosie.getId(), is(10));
    assertThat(rosie.getName(), is("Rosie"));
    assertThat(rosie.isPower(), is(true));
  }

  @Test
  public void testManager() {
    Manager frank = managerBuilder()
        .id(7)
        .name("Frank");
    Manager otherFrank = managerToBuilder(frank)
        .id(9)
        .name("Frank N.")
        .build();
    assertThat(frank.getId(), is(7));
    assertThat(frank.getName(), is("Frank"));
    assertThat(otherFrank.getId(), is(9));
    assertThat(otherFrank.getName(), is("Frank N."));
  }

  @Test
  public void testMyCalendar() {
    MyCalendar calendar = calendarBuilder()
        .unixTime(987128376)
        .dst(true)
        .currentYear("2015");
    MyCalendar updatedCalendar = calendarToBuilder(calendar)
        .unixTime(987128377)
        .dst(false)
        .currentYear("2017")
        .build();
    assertThat(calendar.getUnixTime(), is(987128376l));
    assertThat(calendar.isDst(), is(true));
    assertThat(calendar.getCurrentYear(), is("2015"));
    assertThat(updatedCalendar.getUnixTime(), is(987128377l));
    assertThat(updatedCalendar.isDst(), is(false));
    assertThat(updatedCalendar.getCurrentYear(), is("2017"));
  }

}