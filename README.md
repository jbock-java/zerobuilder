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

Add a `@Builder` annotation to any method or constructor. You can also add an `@Updater` annotation, if your type has "projections" 
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

### Why zero?

If the optional `@Builders(recycle = true)` annotation is present on the enclosing type,
<em>and the goal method does not use any type variables</em>, 
the generated code will reuse the generated builder/updater instance(s).
Thus, the project name implies "zero garbage collection".
