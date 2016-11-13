package net.zerobuilder.examples.generics;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;
import java.util.Map.Entry;

import static net.zerobuilder.examples.generics.TestStepBuilders.entryBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class TestStepTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void entry() throws Exception {
    Entry<String, Integer> entry = entryBuilder()
        .value(12)
        .key("a")
        .suffix(12);
    assertThat(entry.getKey(), is("a12"));
    assertThat(entry.getValue(), is(12));
  }

  @Test
  public void nullKey() throws Exception {
    exception.expect(NullPointerException.class);
    exception.expectMessage("key");
    entryBuilder().value(12).key(null).suffix(12);
  }
}