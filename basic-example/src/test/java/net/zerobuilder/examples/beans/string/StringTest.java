package net.zerobuilder.examples.beans.string;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static net.zerobuilder.examples.beans.string.StringBuilders.stringBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringTest {

  @Test
  public void getStrings() {
    String initialString = new String();
    List<String> strings = List.of(initialString);
    String string = stringBuilder().string(strings);
    assertEquals(1, string.getString().size());
    assertEquals(initialString, string.getString().get(0));
  }
}
