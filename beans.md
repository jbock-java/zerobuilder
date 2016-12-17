# zerobuilder for beans

### Why?

Despite the functional additions that Java 8 brought to the JDK,
traditional JavaBeans, also known as POJOs, are still in widespread use.

Certain frameworks like [JAXB](https://jaxb.java.net/) and
[JPA](https://en.wikipedia.org/wiki/Java_Persistence_API) require the bean standard.

After construction, a bean must be modified <em>in-place</em> via setters.
This often leads to imperative, mutation-based code.

One way to avoid destructive updates is by calling `clone` on your bean.
But this creates a deep copy.
Shallow copies are cheaper and often sufficient.

### How?

The builder code will be generated if you add the `@BeanBuilder` to a POJO class:

````java
@BeanBuilder
public class BusinessAnalyst {
  private String name;
  private int age;

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public int getAge() { return age; }
  public void setAge(int age) { this.age = age; }
}
````

A class `BusinessAnalystBuilders` will be generated.
This class has two static methods that can be used to create and "update" instances of `BusinessAnalystBuilder`.

````java
@Generated
public final class BusinessAnalystBuilders {
  public static BusinessAnalystBuilder.Age businessAnalystBuilder() { ... }
  public static BusinessAnalystUpdater businessAnalystToUpdater(BusinessAnalyst businessAnalyst) { ... }
}
````

### Order of steps

The `static businessAnalystBuilder()` method returns an interface called `Age`.
This is the first step in a linear "chain" of interfaces that ends in an instance of `BusinessAnalyst`.

By default, the builder steps are in alphabetic order.
This can be overridden by adding a `@GetterOrder` annotation to one of the getters:

````java
@GetterOrder(0)
public String getName() { 
  return name; 
}
````

Now `name` is the first step.
The remaining steps `age`, `executive` and `notes` are still in alphabetic order.
In order to make `notes` the second step, add `@GetterOrder(1)` to the corresponding getter, and so on.

### Null checking

Runtime null checks can be generated with a `@NotNullGetter` annotation on getters.

````java
@NotNullGetter
public String getName() {
  return name;
}
````

The getter is now guaranteed to never return null, under one condition: 
This only applies if the bean was created or updated using zerobuilder,
and its setters never directly invoked. 

This condition is why a standard `@NotNull` annotation would be misleading.

### Ignoring a method

Sometimes a bean may have a getter method without a corresponding setter.
There's special logic if this getter returns a `java.util.List`, 
and the `getClass` method inherited from `java.lang.Object` is also ignored. 
Otherwise it's an error if the setter doesn't exist.

The `@IgnoreGetter` annotation can be used to ignore the getter altogether:

````java
@IgnoreGetter
public String getFoo() { 
  return "foo";
}
````
