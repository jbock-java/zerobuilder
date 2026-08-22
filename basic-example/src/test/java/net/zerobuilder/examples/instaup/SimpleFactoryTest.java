package net.zerobuilder.examples.instaup;

import net.zerobuilder.examples.instaup.SimpleFactory.Simple;
import net.zerobuilder.examples.instaup.SimpleFactoryBuilders.SimpleUpdater;
import net.zerobuilder.examples.instaup.SimpleFactoryBuilders.SimpleUpdaterFactory;
import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.instaup.SimpleFactoryBuilders.simpleUpdaterFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleFactoryTest {

  private final SimpleFactory factory = new SimpleFactory("a");
  private final SimpleUpdaterFactory updaterFactory = simpleUpdaterFactory(factory);

  @Test
  void simple() throws Exception {
    Simple simple = factory.simple("b");
    SimpleUpdater updater = updaterFactory.updater(simple);
    Simple updatedSimple = updater
        .appendix("c")
        .build();
    assertEquals("ab", simple.concat());
    assertEquals("ac", updatedSimple.concat());
  }
}
