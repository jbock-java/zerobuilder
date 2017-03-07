# A flexible builder scheme

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/zerobuilder/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/zerobuilder)

Zerobuilder is an annotation processor that generates the <em>builder pattern</em>.
There are two different types of use cases:

* Creating and updating instances of immutable value objects.
  See the quick start below, and the [detailed documentation](values.md).
* Safe "update" of mutable JavaBeans, by creating shallow copies.
  There's a separate [documentation for beans](beans.md).

### Non goals

Zerobuilder does not generate your "business" data objects, such as beans, or (hopefully) immutable objects.
There are many other tools that do this, such as 
[auto-value](https://github.com/google/auto/tree/master/value)
and [derive4j](https://github.com/derive4j/derive4j). 
Zerobuilder can work "on top" of these. See [here](values.md#auto-value) for an example with auto-value.

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

The generateed code references the annotated class, but is self contained otherwise.
In a maven pom, it should have `provided` scope:
 
````xml
<dependency>
    <groupId>com.github.h908714124</groupId>
    <artifactId>zerobuilder</artifactId>
    <version>1.631</version>
    <scope>provided</scope>
</dependency>
````

### Migrating to 1.6xx

The `@Builders` and `@Goal` annotations are gone. Use `@Builder`, `@Updater` and `@Recycle` instead.
`@NonNull` was dropped in 1.631; please use [zerobuilder on top of auto-value](values.md#auto-value)
if you need this functionality.
