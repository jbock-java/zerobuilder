package net.zerobuilder.examples.basic;

import net.zerobuilder.examples.basic.SimpleNull.BasicNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static net.zerobuilder.examples.basic.SimpleNull_BasicNullBuilders.basicNullToBuilder;

public class SimpleNullTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testUpdate() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("string");
    BasicNull invalid = new BasicNull(null);
    basicNullToBuilder(invalid).build();
  }
}