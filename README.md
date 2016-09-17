# A flexible builder scheme

This project has two different use cases:

* Create and update immutable value objects. 
  See the [documentation for values](values.md).
* Make it easier to create mutable Java Beans and to treat them as if they were immutable, 
  so that they can be used with confidence as data in Java 8 streams.
  See the [documentation for beans](beans.md).

### Maven

````xml
<dependency>
    <groupId>com.github.h908714124</groupId>
    <artifactId>zerobuilder</artifactId>
    <version>1.301</version>
    <scope>provided</scope>
</dependency>
````
