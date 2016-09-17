package net.zerobuilder.examples.beans.more;

import org.junit.Test;

import java.util.Arrays;

import static net.zerobuilder.examples.beans.more.BusinessAnalystBuilders.businessAnalystBuilder;
import static net.zerobuilder.examples.beans.more.BusinessAnalystBuilders.businessAnalystToBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class BusinessAnalystTest {

  @Test
  public void testToBuilder() throws Exception {
    BusinessAnalyst peter = businessAnalystBuilder()
        .age(36)
        .name("Peter")
        .strings(Arrays.asList("entry"));
    peter.setName("Peter");
    peter.setAge(36);
    BusinessAnalyst upated = businessAnalystToBuilder(peter)
        .age(37)
        .strings(Arrays.asList("entry0", "entry1"))
        .build();
    assertThat(peter.getAge(), is(36));
    assertThat(peter.getName(), is("Peter"));
    assertThat(peter.getStrings(), is(Arrays.asList("entry")));
    assertThat(upated.getAge(), is(37));
    assertThat(upated.getName(), is("Peter"));
    assertThat(upated.getStrings(), is(Arrays.asList("entry0", "entry1")));
  }
}