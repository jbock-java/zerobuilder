## Zerobuilder for beans

### Why?

Despite the functional additions that Java 8 brought to the JDK,
classic JavaBeans, also known as POJOs, are still in widespread use.

Certain frameworks like [JAXB](https://jaxb.java.net/) and
[JPA](https://en.wikipedia.org/wiki/Java_Persistence_API) require the bean standard.

By definition, a bean always has a public default constructor, and can only be manipulated via setters.
This kind of datatype is inherently tied to a programming model based on mutation.

The situation changes if all _create_ and _update_ operations are handled by zerobuilder:

* Beans are created as if all properties had to be passed to the constructor. 
  That means that code which creates a bean without invoking _every_ setter will not compile. 
* Beans are updated as if they were immutable.
  The generated `static beanToBuilder(Bean bean)` creates a shallow copy. 
  The original bean is never modified.

### How?

Zerobuilder will not extend your classes. It generates code that helps you work with them.

This happens if you add two annotations `@Builders` and `@Goal` to a POJO class:

````java
@Builders(recycle = true)
@Goal(toBuilder = true)
public class BusinessAnalyst {
  private String name;
  private int age;

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public int getAge() { return age; }
  public void setAge(int age) { this.age = age; }
}
````

If zerobuilder is in the classpath, a class `BusinessAnalystBuilders` will be generated.
This class has two static methods that can be used to create and "update" instances of `BusinessAnalystBuilder`.

````java
@Generated
public final class BusinessAnalystBuilders {
  public static BusinessAnalystBuilder.Age businessAnalystBuilder() { ... }
  public static BusinessAnalystUpdater businessAnalystToBuilder(BusinessAnalyst businessAnalyst) { ... }
}
````

If you clone this project and do a `mvn install`, you will find the complete source code of `BusinessAnalystBuilders.java`
in the `examples/basic/target/generated-sources/annotations` folder.

To see how the generated class is used, have a look at
[BusinessAnalystTest](examples/basic/src/test/java/net/zerobuilder/examples/beans/more/BusinessAnalystTest.java).

### Recycle

Creating and discarding an intermediate builder object for every create or update operation 
may create additional pressure on the garbage collector.

If you feel that zerobuilder affects performance,
you could try the `recycle = true` option.
This will cause the builder instances to be stored in a `ThreadLocal` and reused.

### Order of steps

The `static businessAnalystBuilder()` method returns an interface called `Age`.
This is the first step in a linear "chain" of interfaces that ends in an instance of `BusinessAnalyst`.

By default, the builder steps are in alphabetic order, which is why the `age` property
has to be specified first.

The alphabetic order can be overridden by adding a `@Step` annotation to one of the getters:

````java
@Step(0)
public String getName() { 
  return name; 
}
````

Now `name` is the first step.
The remaining steps `age`, `executive` and `notes` are still in alphabetic order.
In order to make `notes` the second step, add `@Step(1)` to the corresponding getter, and so on.

### Null checking

Null checking is enabled by adding a `@Step` annotation to a getter:

````java
@Step(nonNull = true)
public String getName() {
  return name;
}
````

It is also possible to have all non-primitive properties checked,
by using the goal level `nonNull` option:

````java
@Builders
@Goal(nonNull = true)
public class Employee { ... }
````

In this case, individual getters can be excluded with `@Step(nonNull = false)`.

### Lone getter

In handwritten beans, you may occasionally have a getter without a corresponding setter.

````java
public String getFoo() {
  return "foo";
}
````

Zerobuilder will refuse to process this class.
To get around this, add the `@Ignore` annotation:

````java
@Ignore
public String getFoo() { 
  return "foo";
}
````

### Lone getter that returns a collection

Some tools (such as [wsdl2java](https://cxf.apache.org/docs/wsdl-to-java.html)) can generate beans with lone getters that look like this:

````java
public List<Employee> getEmployees() {
  if (this.employees == null) {
    this.employees = new ArrayList<Employee>();
  }
  return this.employees;
}
````

In this case (lone getter that returns a subclass of `Collection`),
it is often not necessary to ignore the getter.
Zerobuilder will assume that the collection is mutable and adapt the builder code accordingly.

