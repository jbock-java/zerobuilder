package net.zerobuilder.examples.values;

import net.zerobuilder.RecordBuilder;

final class ProjectionPrecedence {

  private static abstract class Base {
    final String foo;

    private Base(String foo) {
      this.foo = foo;
    }
  }

  @RecordBuilder
  static final class InheritedField extends Base {

    InheritedField(String foo) {
      super(foo);
    }

    String getFoo() {
      throw new AssertionError();
    }

    String foo() {
      throw new AssertionError();
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

    String getFoo() {
      return foo;
    }

    private String foo() {
      throw new AssertionError();
    }

    String isFoo() {
      throw new AssertionError();
    }
  }

  @RecordBuilder
  static final class BoolGetter {

    private final boolean foo;

    BoolGetter(boolean foo) {
      this.foo = foo;
    }

    boolean getFoo() {
      throw new AssertionError();
    }

    boolean isFoo() {
      return foo;
    }
  }

  private ProjectionPrecedence() {
    throw new UnsupportedOperationException("no instances");
  }
}
