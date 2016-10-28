package net.zerobuilder.examples.paperclips;

import net.zerobuilder.paperclips.PaperclipFactory;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static net.zerobuilder.paperclips.EmployeeBuilders.employeeBuilder;
import static net.zerobuilder.paperclips.PaperclipFactoryBuilders.paperclipFactoryBuilder;
import static net.zerobuilder.paperclips.PaperclipFactoryBuilders.paperclipFactoryUpdater;
import static net.zerobuilder.paperclips.PaperclipFactory_EmployeesBuilders.employeesBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PaperclipTest {

  @Test
  public void factoryTest() {
    PaperclipFactory factory =
        paperclipFactoryBuilder()
            .employees(employeesBuilder().employees(singletonList(
                employeeBuilder().handle("Sven"))))
            .size(1000000000)
            .type("Stationary");
    PaperclipFactory updated =
        paperclipFactoryUpdater(factory)
            .size(5)
            .type("Embedded")
            .done();
    assertThat(factory.getSize(), is(1000000000));
    assertThat(factory.getType(), is("Stationary"));
    assertThat(updated.getSize(), is(5));
    assertThat(updated.getType(), is("Embedded"));
  }
}
