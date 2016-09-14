package net.zerobuilder.examples.beans.more;

import org.junit.Test;

import static net.zerobuilder.examples.beans.more.BusinessAnalystBuilders.businessAnalystToBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class BusinessAnalystTest {

  @Test
  public void testToBuilder() throws Exception {
    BusinessAnalyst peter = new BusinessAnalyst();
    peter.setName("Peter");
    peter.setAge(36);
    BusinessAnalyst upated = businessAnalystToBuilder(peter)
        .age(37)
        .build();
    assertThat(peter.getAge(), is(36));
    assertThat(peter.getName(), is("Peter"));
    assertThat(upated.getAge(), is(37));
    assertThat(upated.getName(), is("Peter"));
  }
}