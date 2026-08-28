package net.zerobuilder.examples.values;

import net.zerobuilder.RecordBuilder;

final class ProjectionPrecedence {

  @RecordBuilder
  static final class InheritedField {
    final String foo;

    InheritedField(String foo) {
      this.foo = foo;
    }

    String getFoo() {
      throw new AssertionError();
    }

    String foo() {
      return foo;
    }

    String isFoo() {
      throw new AssertionError();
    }
  }

  @RecordBuilder
  static final class Getter {

    private final String foo;

    Getter(String foo) {
      this.foo = foo;
    }

    String getFoo() {
      throw new AssertionError();
    }

    String foo() {
      return foo;
    }

    String isFoo() {
      throw new AssertionError();
    }
  }

  @RecordBuilder
  static final class AutoGetter {

    private final String foo;

    AutoGetter(String foo) {
      this.foo = foo;
    }

    String foo() {
      return foo;
    }
  }

  @RecordBuilder
  static final class BoolGetter {

    private final boolean foo;

    BoolGetter(boolean foo) {
      this.foo = foo;
    }

    boolean foo() {
      return foo;
    }
  }

  private ProjectionPrecedence() {
  }
}
