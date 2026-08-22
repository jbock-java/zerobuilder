package net.zerobuilder.examples.values;

import net.zerobuilder.examples.values.FieldAccessBuilders.FieldAccessBuilder;
import net.zerobuilder.examples.values.FieldAccessBuilders.FieldAccessUpdater;
import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.values.FieldAccessBuilders.fieldAccessBuilder;
import static net.zerobuilder.examples.values.FieldAccessBuilders.fieldAccessUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldAccessTest {

  @Test
  void basicTest() {
    FieldAccessBuilder.Length builder = fieldAccessBuilder();
    FieldAccess original = builder.length(12).width(10).height(11);
    FieldAccessUpdater updater = fieldAccessUpdater(original);
    FieldAccess updated = updater.length(0).build();
    assertEquals(12d, original.length);
    assertEquals(10d, original.width);
    assertEquals(11d, original.height);
    assertEquals(0d, updated.length);
    assertEquals(10d, updated.width);
    assertEquals(11d, updated.height);
  }
}
