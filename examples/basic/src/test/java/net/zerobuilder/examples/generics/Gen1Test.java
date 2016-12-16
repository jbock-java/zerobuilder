package net.zerobuilder.examples.generics;

import net.zerobuilder.examples.generics.Gen1.Bar;
import org.junit.Test;

import static net.zerobuilder.examples.generics.Gen1Builders.barBuilder;
import static net.zerobuilder.examples.generics.Gen1Builders.barUpdaterFactory;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class Gen1Test {

  @Test(expected = NullPointerException.class)
  public void nullBuilder() {
    barBuilder(new Gen1<>("1", "2"))
        .bc0(3)
        .bc1(null)
        .bd0(5)
        .bd1(6)
        .bc2(7);
  }

  @Test(expected = NullPointerException.class)
  public void nullUpdater() {
    Gen1<String, String> factory = new Gen1<>("1", "2");
    Bar<String, String, Integer, Integer> bar = barBuilder(factory)
        .bc0(3)
        .bc1(4)
        .bd0(5)
        .bd1(6)
        .bc2(7);
    barUpdaterFactory(factory)
        .updater(bar)
        .bd1(null)
        .done();
  }

  @Test(expected = NullPointerException.class)
  public void sneakyNull() {
    Gen1<String, String> factory = new Gen1<>("1", "2");
    Bar<String, String, Integer, Integer> bar = new Gen1.Bar<>("1", "2", null, 4, 5, 6, 7);
    barUpdaterFactory(factory)
        .updater(bar)
        .done();
  }

  @Test
  public void sneakyNullAmended() {
    Gen1<String, String> factory = new Gen1<>("1", "2");
    Bar<String, String, Integer, Integer> bar = new Gen1.Bar<>("1", "2", null, 4, 5, 6, 7);
    Bar<String, String, Integer, Integer> updated = barUpdaterFactory(factory)
        .updater(bar)
        .bc0(3)
        .done();
    assertThat(updated.bc0, is(3));
  }
}