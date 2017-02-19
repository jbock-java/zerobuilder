package net.zerobuilder.examples.generics;

import net.zerobuilder.examples.generics.Gen1.Bar;
import org.junit.Test;

import static net.zerobuilder.examples.generics.Gen1Builders.barBuilder;
import static net.zerobuilder.examples.generics.Gen1Builders.barUpdaterFactory;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class Gen1Test {

  @Test
  public void notDone() {
    Gen1<String, String> factory = new Gen1<>("1", "2", "someString");
    Bar<String, String, Integer, Integer> bar = barBuilder(factory)
        .bc0(3)
        .bc1(4)
        .bd0(5)
        .bd1("6")
        .bc2(7);
    Bar<String, String, Integer, Integer> updated = barUpdaterFactory(factory)
        .updater(bar)
        .bd1(null)
        .bd1("10")
        .done();
    assertThat(updated.bd1, is("10"));
  }

  @Test
  public void temporaryNull() {
    Gen1<String, String> factory = new Gen1<>("1", "2", "someString");
    Bar<String, String, Integer, Integer> bar = new Gen1.Bar<>("1", "someString", "2", null, 4, 5, "6", 7);
    Bar<String, String, Integer, Integer> updated = barUpdaterFactory(factory)
        .updater(bar)
        .bc0(3)
        .done();
    assertThat(updated.bc0, is(3));
  }
}