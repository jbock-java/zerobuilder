package net.zerobuilder.examples.values;

import net.zerobuilder.Updater;

final class ProjectionPrecedence {

  private static abstract class Base {
    final String foo;

    private Base(String foo) {
      this.foo = foo;
    }
  }

  static final class InheritedField extends Base {

    @Updater
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

  static final class Getter {

    private final String foo;

    @Updater
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

  static final class AutoGetter {

    private final String foo;

    @Updater
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

  static final class BoolGetter {

    private final boolean foo;

    @Updater
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
