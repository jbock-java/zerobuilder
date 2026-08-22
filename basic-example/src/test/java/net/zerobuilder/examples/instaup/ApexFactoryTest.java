package net.zerobuilder.examples.instaup;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.instaup.ApexFactoryBuilders.apexUpdaterFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ApexFactoryTest {

  @Test
  void apex() throws SQLException {
    ApexFactory<String> factory = new ApexFactory<>("a");
    ApexFactory.Apex<String> apex = factory.apex("b");
    ApexFactory.Apex<String> updatedApex = apexUpdaterFactory(factory)
        .updater(apex)
        .appendix("c")
        .build();
    assertEquals("ab", apex.concat());
    assertEquals("ac", updatedApex.concat());
  }
}
