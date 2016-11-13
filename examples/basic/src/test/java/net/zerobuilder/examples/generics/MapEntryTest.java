package net.zerobuilder.examples.generics;

import org.junit.Test;

import java.util.Map.Entry;

import static net.zerobuilder.examples.generics.MapEntryBuilders.entryBuilder;
import static net.zerobuilder.examples.generics.MapEntryBuilders.sentryBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MapEntryTest {

  @Test
  public void entry() throws Exception {
    Entry<String, Long> entry = entryBuilder()
        .key("foo")
        .value(12L);
    assertThat(entry.getKey(), is("foo"));
    assertThat(entry.getValue(), is(12L));
  }

  @Test
  public void sentry() throws Exception {
    Entry<String, String> entry = sentryBuilder()
        .key("foo")
        .value("bar");
    assertThat(entry.getKey(), is("foo"));
    assertThat(entry.getValue(), is("bar"));
  }
}