package net.zerobuilder.examples.values;

import net.zerobuilder.examples.values.FieldAccessBuilders.FieldAccessBuilder;
import net.zerobuilder.examples.values.FieldAccessBuilders.FieldAccessUpdater;
import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.values.FieldAccessBuilders.fieldAccessBuilder;
import static net.zerobuilder.examples.values.FieldAccessBuilders.fieldAccessUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FieldAccessTest {

  @Test
  public void basicTest() {
    FieldAccessBuilder.Length builder = fieldAccessBuilder();
    FieldAccess original = builder.length(12).width(10).height(11);
    FieldAccessUpdater updater = fieldAccessUpdater(original);
    FieldAccess updated = updater.length(0).done();
    assertEquals(original.length, 12d);
    assertEquals(original.width, 10d);
    assertEquals(original.height, 11d);
    assertEquals(updated.length, 0d);
    assertEquals(updated.width, 10d);
    assertEquals(updated.height, 11d);
    // check that caching works
    assertTrue(builder == fieldAccessBuilder());
    assertTrue(updater == fieldAccessUpdater(original));
  }
}
