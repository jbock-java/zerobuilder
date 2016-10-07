package net.zerobuilder.examples.beans.more;

import net.zerobuilder.Builders;
import net.zerobuilder.Goal;
import net.zerobuilder.Step;

import java.util.ArrayList;
import java.util.List;

import static net.zerobuilder.NullPolicy.ALLOW;
import static net.zerobuilder.NullPolicy.REJECT;

// demonstration of different null checking behaviour, depending on whether a collection has a setter
// see NullChecksTest
public class NullChecks {

  // setter is missing -> elements are checked
  @Builders
  @Goal
  static class CheckedCollection {
    private List<String> strings;
    @Step(nullPolicy = REJECT)
    List<String> getStrings() {
      if (strings == null) {
        strings = new ArrayList<>();
      }
      return strings;
    }
  }

  // elements are not checked
  @Builders
  @Goal
  public static class NullableElements {
    private List<String> strings;
    @Step
    public List<String> getStrings() {
      if (strings == null) {
        strings = new ArrayList<>();
      }
      return strings;
    }
  }

  // setter is present -> collection itself is checked but not its elements
  // to check elements, pass ImmutableList or other null-rejecting type to the setter
  @Builders
  @Goal
  public static class UncheckedCollection {
    private List<String> strings;
    @Step(nullPolicy = REJECT)
    public List<String> getStrings() {
      return strings;
    }
    public void setStrings(List<String> strings) {
      this.strings = strings;
    }
  }

  // not a collection
  @Builders
  @Goal
  public static class CheckedString {
    private String string;
    @Step(nullPolicy = REJECT)
    public String getString() {
      return string;
    }
    public void setString(String string) {
      this.string = string;
    }
  }

  // goal-level nonNull
  @Builders
  @Goal(nullPolicy = REJECT)
  public static class Default {
    private String foo;
    private String bar;
    @Step(nullPolicy = ALLOW)
    public String getFoo() {
      return foo;
    }
    public void setFoo(String foo) {
      this.foo = foo;
    }
    public String getBar() {
      return bar;
    }
    public void setBar(String bar) {
      this.bar = bar;
    }
  }
}
