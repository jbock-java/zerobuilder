# A flexible builder scheme

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/zerobuilder/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/zerobuilder)

This project generates some variants of the builder pattern.
It has two different use cases:

* Create and update <em>instances of</em> immutable value objects with minimal effort. 
  See [values](values.md).
* Create <em>instances of</em> mutable JavaBeans and update them with "immutable semantics", i.e. by making shallow copies.
  See [beans](beans.md).

### Non goals

* Generating data types. This is up to the user, or other tools (see [examples](examples)).
  Thanks for [mentioning us](https://github.com/jodastephen/compare-beangen) though!

### Quick start

Add a `@Goal` annotation to any non-private method or constructor:

````java
  final class Doo {
    final String foo;
    final String bar;
    @Goal(updater = true)
    Doo(String foo, String bar) {
      this.foo = foo;
      this.bar = bar;
    }
  }
````

This will generate a class called `DooBuilders` in the same package.
Note that `updater = true` does not work everywhere.

### Why zero?

If the `recycle` flag is set, <em>and the goal method does not use any type variables</em>, 
the generated code will reuse the generated builder/updater instance(s).
Thus, the project name implies "zero gc".
