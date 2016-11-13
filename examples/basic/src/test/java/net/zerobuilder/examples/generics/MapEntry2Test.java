package net.zerobuilder.examples.generics;

import org.junit.Test;

import java.util.Map.Entry;

import static net.zerobuilder.examples.generics.MapEntry2Builders.entryBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MapEntry2Test {

  @Test
  public void sentry() throws Exception {
    Entry<String, String> entry = entryBuilder()
        .key("foo")
        .value("bar");
    assertThat(entry.getKey(), is("foo"));
    assertThat(entry.getValue(), is("bar"));
  }
}