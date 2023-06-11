package net.zerobuilder.examples.instaup;

import net.zerobuilder.examples.instaup.SimpleFactoryBuilders.SimpleUpdaterFactory;
import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.instaup.SimpleFactoryBuilders.simpleUpdaterFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class SimpleFactoryTest {

  private final SimpleFactory factory = new SimpleFactory("a");
  private final SimpleUpdaterFactory updaterFactory = simpleUpdaterFactory(factory);

  @Test
  public void simple() throws Exception {
    SimpleFactory.Simple simple = factory.simple("b");
    SimpleFactoryBuilders.SimpleUpdater updater = updaterFactory.updater(simple);
    SimpleFactory.Simple updatedSimple = updater
        .appendix("c")
        .done();
    assertEquals(simple.concat(), "ab");
    assertEquals(updatedSimple.concat(), "ac");
    // updater is recycled
    assertSame(updaterFactory.updater(factory.simple("x")), updater);
  }
}
