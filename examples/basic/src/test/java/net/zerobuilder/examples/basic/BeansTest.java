package net.zerobuilder.examples.basic;

import net.zerobuilder.examples.beans.Accountant;
import net.zerobuilder.examples.beans.Employee;
import net.zerobuilder.examples.beans.Manager;
import net.zerobuilder.examples.beans.User;
import org.junit.Test;

import static net.zerobuilder.examples.basic.BeansBuilders.employeeBuilder;
import static net.zerobuilder.examples.basic.BeansBuilders.userBuilder;
import static net.zerobuilder.examples.basic.BeansBuilders.userToBuilder;
import static net.zerobuilder.examples.basic.Beans_MoreBeansBuilders.accountantBuilder;
import static net.zerobuilder.examples.basic.Beans_MoreBeansBuilders.managerBuilder;
import static net.zerobuilder.examples.basic.Beans_MoreBeansBuilders.managerToBuilder;
import static net.zerobuilder.examples.basic.Beans_MoreBeansBuilders.otherAccountantBuilder;
import static net.zerobuilder.examples.basic.Beans_MoreBeansBuilders.otherAccountantToBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BeansTest {

  @Test
  public void testEmployee() {
    Employee employee = employeeBuilder()
        .id(4711)
        .name("Herbert")
        .salary(60 * 1000);
    assertThat(employee.getId(), is(4711));
    assertThat(employee.getName(), is("Herbert"));
    assertThat(employee.getSalary(), is(60 * 1000));
  }

  @Test
  public void testUser() {
    User ben = userBuilder()
        .id(10)
        .name("Ben");
    User rosie = userToBuilder(ben)
        .name("Rosie")
        .build();
    assertThat(ben.getId(), is(10));
    assertThat(ben.getName(), is("Ben"));
    assertThat(rosie.getId(), is(10));
    assertThat(rosie.getName(), is("Rosie"));
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
  public void testAccountant() {
    Accountant accountant = accountantBuilder()
        .id(1)
        .name("Simone")
        .salary(90 * 1000)
        .account("0181");
    assertThat(accountant.getId(), is(1));
    assertThat(accountant.getName(), is("Simone"));
    assertThat(accountant.getSalary(), is(90 * 1000));
    assertThat(accountant.getAccount(), is("0181"));
  }

  @Test
  public void testOtherAccountant() {
    net.zerobuilder.examples.beans.more.Accountant simon = otherAccountantBuilder()
        .id(1)
        .name("Simon")
        .salary(100 * 1000)
        .account("0159");
    net.zerobuilder.examples.beans.more.Accountant tetsuo = otherAccountantToBuilder(simon)
        .id(1000)
        .name("Tetsuo")
        .build();
    assertThat(simon.getId(), is(1));
    assertThat(simon.getName(), is("Simon"));
    assertThat(simon.getSalary(), is(100 * 1000));
    assertThat(simon.getAccount(), is("0159"));
    assertThat(tetsuo.getId(), is(1000));
    assertThat(tetsuo.getName(), is("Tetsuo"));
    assertThat(tetsuo.getSalary(), is(100 * 1000));
    assertThat(tetsuo.getAccount(), is("0159"));
  }

}