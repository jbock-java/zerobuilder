# A flexible builder scheme

This project has two different use cases:

* Create and update immutable value objects. 
  See the [documentation for values](values.md).
* Make it easier to create mutable Java Beans and to treat them as if they were immutable, 
  so that they can be used with confidence as data in Java 8 streams.
  See the [documentation for beans](beans.md).

### Use it in your project

This is a standard annotation processor, compatible with Java 7 and higher.
No compiler plugin or special IDE is needed.
However, maven compiler plugin version `3.5.1` or greater is recommended.

Your IDE may need some initial help, to recognize that `target/generated-sources/annotations`
now contains generated sources.

<em>Tip for intellij users:</em> If you do a `mvn install` before opening one of the example projects,
intellij will recognize `target/generated-sources/annotations` automatically.

### Maven

````xml
<dependency>
    <groupId>com.github.h908714124</groupId>
    <artifactId>zerobuilder</artifactId>
    <version>1.411</version>
    <scope>provided</scope>
</dependency>
````
