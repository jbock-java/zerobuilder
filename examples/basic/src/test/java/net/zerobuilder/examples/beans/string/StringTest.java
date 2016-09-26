package net.zerobuilder.examples.beans.string;

import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static net.zerobuilder.examples.beans.string.StringBuilders.stringBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StringTest {
  @Test
  public void getStrings() throws Exception {
    String initialString = new String();
    List<String> strings = asList(initialString);
    String string = stringBuilder().string(strings);
    assertThat(string.getString().size(), is(1));
    assertThat(string.getString().get(0), is(initialString));
  }
}