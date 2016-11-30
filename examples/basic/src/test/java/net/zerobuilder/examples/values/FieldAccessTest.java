package net.zerobuilder.examples.values;

import net.zerobuilder.examples.values.FieldAccessBuilders.FieldAccessBuilder;
import net.zerobuilder.examples.values.FieldAccessBuilders.FieldAccessUpdater;
import org.junit.Test;

import static net.zerobuilder.examples.values.FieldAccessBuilders.fieldAccessBuilder;
import static net.zerobuilder.examples.values.FieldAccessBuilders.fieldAccessUpdater;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FieldAccessTest {

  @Test
  public void basicTest() {
    FieldAccessBuilder.Length builder = fieldAccessBuilder();
    FieldAccess original = builder.length(12).width(10).height(11);
    FieldAccessUpdater updater = fieldAccessUpdater(original);
    FieldAccess updated = updater.length(0).done();
    assertThat(original.length, is(12d));
    assertThat(original.width, is(10d));
    assertThat(original.height, is(11d));
    assertThat(updated.length, is(0d));
    assertThat(updated.width, is(10d));
    assertThat(updated.height, is(11d));
    // check that caching works
    assertTrue(builder == fieldAccessBuilder());
    assertTrue(updater == fieldAccessUpdater(original));
  }
}