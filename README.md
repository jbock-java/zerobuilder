# A flexible builder scheme

This project has two different use cases:

* Create and update <em>instances of</em> immutable value objects with minimal effort. 
  See [values](values.md).
* Create <em>instances of</em> mutable JavaBeans and update them with "immutable semantics", i.e. by making shallow copies.
  See [beans](beans.md).

### Non goals

* Generating data types. This is up to the user, or other tools (see [examples](examples)).
  Thanks for [mentioning us](https://github.com/jodastephen/compare-beangen) though!

### How to use

This is a standard Java &ge; 7 annotation processor.
The generated code has no runtime dependencies.
After compilation, the annotated source does not depend on zerobuilder; see
[RetentionPolicy.SOURCE](https://docs.oracle.com/javase/7/docs/api/java/lang/annotation/RetentionPolicy.html#SOURCE).

Maven compiler plugin version `3.5.1` or greater is recommended.

Your IDE may need some initial help, to recognize that `target/generated-sources/annotations`
now contains generated sources.

<em>Tip for intellij users:</em> If you do a `mvn install` before opening one of the example projects,
intellij will recognize `target/generated-sources/annotations` automatically.

### Java API

The core functionality is available as a separate library. See [api](api).

### Why zero?

Because using the generated builders has zero impact on garbage collection, if the `recycle` option is used.
In this case, the intermediate builder objects are stored in `ThreadLocal` instances and reused.

### Maven

````xml
<dependency>
    <groupId>com.github.h908714124</groupId>
    <artifactId>zerobuilder</artifactId>
    <version>1.481</version>
    <scope>provided</scope>
</dependency>
````
