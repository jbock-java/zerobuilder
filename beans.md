# zerobuilder for beans

### Why?

Despite the functional additions that Java 8 brought to the JDK,
traditional JavaBeans, also known as POJOs, are still in widespread use.

Furthermore, some frameworks like [JAXB](https://jaxb.java.net/) and
[JPA](https://en.wikipedia.org/wiki/Java_Persistence_API) still require the bean standard.

Isn't there some way around calling all these setters?
After all, someone recently told us that mutation is bad.

### How to manage

The javacode that solves all your problems will be generated _for free_,
if you add the `@BeanBuilder` annotation to a JavaBean:

````java
@BeanBuilder
class BusinessAnalyst {
  private String name;
  private int age;
  // 46 more fields

  String getName() { return name; }
  void setName(String name) { this.name = name; }
  int getAge() { return age; }
  void setAge(int age) { this.age = age; }
  // 46 more getter / setter pairs
}
````

A class called `BusinessAnalystBuilders` will be generated in the same package.
`mvn compile` will put the generated java source somewhere under `target`, so be sure to
add that folder to the classpath in your IDE as well.

The generated class has two _important_ static methods:

````java
@Generated
public final class BusinessAnalystBuilders {
  public static BusinessAnalystBuilder.Age businessAnalystBuilder() { ... }
  public static BusinessAnalystUpdater businessAnalystToUpdater(BusinessAnalyst businessAnalyst) { ... }
}
````

These can be used to create new instances, as well as (modified) shallow copies, of `BusinessAnalystBuilder`.

### Order of steps

The `static businessAnalystBuilder()` method returns an interface called `Age`.
This is the first step in a linear "chain" of interfaces that ends in an instance of `BusinessAnalyst`.

By default, the builder steps are in alphabetic order.
This order can be overridden by adding a `@Getter` annotation to one of the getters:

````java
@Getter(0)
getName() { 
  return name; 
}
````

Now `name` will be the first step. Alternatively, `@Getter(1) int getAge()` would have the same effect.

### Ignoring a method

Sometimes a bean may have a getter method without a corresponding setter.
There _is_ special logic if this getter returns a `java.util.List`,
and the `getClass()` method from `java.lang.Object` is also ignored. 
Otherwise it's a compile error if the setter doesn't exist.

The `@IgnoreGetter` annotation can be used in this case, to ignore the getter altogether:

````java
@IgnoreGetter
public String getFoo() { 
  return "foo";
}
````
