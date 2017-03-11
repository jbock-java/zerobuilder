# A flexible builder scheme

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/zerobuilder/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/zerobuilder)

Zerobuilder is an annotation processor that generates the <em>builder pattern</em>.
There are two different kinds of use cases:

* Creating and updating instances of immutable value objects.
  See the quick start below, or the [detailed documentation](values.md).
* Safe "update" of mutable JavaBeans, by creating shallow copies.
  There's a separate [documentation for beans](beans.md).

### Non goals

Zerobuilder does not generate your "business" data objects, such as beans, or (hopefully) immutable objects.
There are many other tools that do this, such as 
[auto-value](https://github.com/google/auto/tree/master/value)
and [derive4j](https://github.com/derive4j/derive4j). 
Zerobuilder is compatible with some of these. See [here](values.md#auto-value) for an example with auto-value.

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

This will generate a class called `DooBuilders` in the same package, with two `static` methods:

* The static method `DooBuilders.dooBuilder()` returns the builder.
* The static method `DooBuilders.dooUpdater(Doo doo)` returns the updater.

### Maven note

The generated code will know about the annotated classes,
but be self-contained otherwise.
In a maven pom, therefore, zerobuilder should either be `optional` or have `provided` scope:

````xml
<dependency>
    <groupId>com.github.h908714124</groupId>
    <artifactId>zerobuilder</artifactId>
    <version>1.632</version>
    <scope>provided</scope>
</dependency>
````

### Migrating to 1.6xx

The `@Builders` and `@Goal` annotations are gone. Use `@Builder`, `@Updater`
and (optionally) `@Recycle` instead.
`@NonNull` was dropped in 1.631; please use [zerobuilder with auto-value](values.md#auto-value)
if you need runtime null-checking.
