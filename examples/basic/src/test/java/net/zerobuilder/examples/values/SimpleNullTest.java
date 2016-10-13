package net.zerobuilder.examples.values;

import net.zerobuilder.examples.values.SimpleNull.BasicNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static net.zerobuilder.examples.values.SimpleNull_BasicNullBuilders.basicNullUpdater;

public class SimpleNullTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testUpdate() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("string");
    BasicNull invalid = new BasicNull(null);
    basicNullUpdater(invalid).done();
  }
}