package net.zerobuilder.examples.instaup;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static net.zerobuilder.examples.instaup.ApexFactoryBuilders.apexUpdaterFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ApexFactoryTest {

  @Test
  public void apex() throws SQLException {
    ApexFactory<String> factory = new ApexFactory("a");
    ApexFactory.Apex<String> apex = factory.apex("b");
    ApexFactory.Apex<String> updatedApex = apexUpdaterFactory(factory)
        .updater(apex)
        .appendix("c")
        .done();
    assertEquals("ab", apex.concat());
    assertEquals("ac", updatedApex.concat());
  }
}
