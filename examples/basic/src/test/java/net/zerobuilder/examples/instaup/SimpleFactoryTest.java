package net.zerobuilder.examples.instaup;

import net.zerobuilder.examples.instaup.SimpleFactoryBuilders.SimpleUpdaterFactory;
import org.junit.Test;

import static net.zerobuilder.examples.instaup.SimpleFactoryBuilders.simpleUpdaterFactory;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class SimpleFactoryTest {

  private final SimpleFactory factory = new SimpleFactory("a");
  private final SimpleUpdaterFactory updaterFactory = simpleUpdaterFactory(factory);

  @Test
  public void simple() throws Exception {
    Simple simple = factory.simple("b");
    SimpleFactoryBuilders.SimpleUpdater updater = updaterFactory.updater(simple);
    Simple updatedSimple = updater
        .appendix("c")
        .done();
    assertThat(simple.concat(), is("ab"));
    assertThat(updatedSimple.concat(), is("ac"));
    // updater is recycled
    assertTrue(updater
        == updaterFactory.updater(factory.simple("x")));
  }

}