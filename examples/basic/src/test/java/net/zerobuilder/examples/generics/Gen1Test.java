package net.zerobuilder.examples.generics;

import net.zerobuilder.examples.generics.Gen1.Bar;
import org.junit.Test;

import static net.zerobuilder.examples.generics.Gen1Builders.barBuilder;

public class Gen1Test {

  @Test(expected = NullPointerException.class)
  public void nullBuilder() throws Exception {
    Bar<String, String, Integer, Integer> bar = barBuilder(new Gen1<>("1", "2"))
        .bc0(3)
        .bc1(null)
        .bd0(5)
        .bd1(6)
        .bc2(7);
  }

  @Test(expected = NullPointerException.class)
  public void nullUpdater() throws Exception {
    Gen1<String, String> factory = new Gen1<>("1", "2");
    Bar<String, String, Integer, Integer> bar = barBuilder(factory)
        .bc0(3)
        .bc1(4)
        .bd0(5)
        .bd1(6)
        .bc2(7);
    Gen1Builders.barUpdaterFactory(factory)
        .updater(bar)
        .bd1(null)
        .done();
  }
}