# A flexible builder scheme

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/zerobuilder/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/zerobuilder)

This project generates some variants of the builder pattern.
It has two different use cases:

* Create and update <em>instances of</em> immutable value objects, as shown below.
  More documentation can be found here: [values](values.md).
* Create <em>instances of</em> mutable JavaBeans and update them with "immutable semantics", i.e. by making shallow copies.
  See here: [beans](beans.md).

### Non goals

* Zerobuilder does not generate data types, i.e. beans or value types. 
  Instead, zerobuilder can be combined with tools that do this, such as 
  [auto-value](https://github.com/google/auto/tree/master/value) 
  (more about this [here](values.md)) 
  and [derive4j](https://github.com/derive4j/derive4j).

### Quick start

Add a `@Builder` annotation to any non-private, non-abstract method or constructor.
You can also add an `@Updater` annotation, if the returned (or constructed) type has "projections" 
(in this case, the fields `foo` and `bar`):

````java
  import net.zerobuilder.Builder;
  import net.zerobuilder.Updater;

  final class Doo {

    final String foo;
    final String bar;

    @Builder
    @Updater
    Doo(String foo, String bar) {
      this.foo = foo;
      this.bar = bar;
    }
  }
````

This will generate a class called `DooBuilders` in the same package.

* The static method `DooBuilders.dooBuilder()` returns the builder.
* The static method `DooBuilders.dooUpdater(Doo doo)` returns the updater.

### Maven note

This annotation processor has no runtime dependencies.
It should be used in `provided` scope, as shown here:
 
````xml
<dependency>
    <groupId>com.github.h908714124</groupId>
    <artifactId>zerobuilder</artifactId>
    <!--<version>...</version>-->
    <scope>provided</scope>
</dependency>
````

### Migrating to 1.6xx

The `@Builders` and `@Goal` annotations are gone. Use `@Builder`, `@Updater` and `@Recycle` instead.
