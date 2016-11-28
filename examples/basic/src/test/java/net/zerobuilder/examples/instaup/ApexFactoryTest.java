package net.zerobuilder.examples.instaup;

import org.junit.Test;

import static net.zerobuilder.examples.instaup.ApexFactoryBuilders.apexUpdater;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ApexFactoryTest {

  @Test
  public void apex() {
    ApexFactory factory = new ApexFactory("a");
    Apex<String> apex = factory.apex("b");
    Apex<String> updatedApex = apexUpdater(factory)
        .updater(apex)
        .appendix("c")
        .done();
    assertThat(apex.concat(), is("ab"));
    assertThat(updatedApex.concat(), is("ac"));
  }
}