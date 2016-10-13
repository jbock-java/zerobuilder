# zerobuilder for beans

### Why?

Despite the functional additions that Java 8 brought to the JDK,
classic JavaBeans, also known as POJOs, are still in widespread use.

Certain frameworks like [JAXB](https://jaxb.java.net/) and
[JPA](https://en.wikipedia.org/wiki/Java_Persistence_API) require the bean standard.

By definition, a bean always has a public default constructor, and can be manipulated <em>in-place</em> via setters.
This kind of datatype is inherently tied to a programming model based on mutation.

One way to avoid the <em>destructive updates</em> is by calling `clone()` on your bean.
But this creates a deep copy, which is often unnecessary.
Zerobuilder's generated `toUpdater` method creates shallow copies.
It is a cheaper and safer alternative to `clone`.

Additionally you can choose to use the generated `builder` method.
These two generated methods together allow to create and "modify" any bean 
without calling any setters,
and without ever updating a bean in-place.

### How?

The builder code will be generated if you add two 
annotations `@Builders` and `@Goal` to a POJO class:

````java
@Builders(recycle = true)
@Goal(updater = true)
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

If you clone this project and do a `mvn install`, you will find the complete source code of `BusinessAnalystBuilders.java`
in the `examples/basic/target/generated-sources/annotations` folder.

To see how the generated class is used, have a look at
[BusinessAnalystTest](examples/basic/src/test/java/net/zerobuilder/examples/beans/more/BusinessAnalystTest.java).

### Recycle

Creating and discarding an intermediate builder object for every create or update operation 
may increase the pressure on the garbage collector.

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

Zerobuilder can insert null checks at runtime for all non-primitive properties.
This works via `@Goal(nullPolicy = NullPolicy.REJECT)` and `@Step(nullPolicy = NullPolicy.REJECT)`.

For details, see the [documentation for values](values.md).

### Ignoring a method

Sometimes a bean may have a method that looks like a getter,
but there is no corresponding setter.

````java
public String getFoo() {
  return "foo";
}
````

Zerobuilder will complain, because this doesn't conform to the bean standard.
The `@Ignore` annotation can be used to make zerobuilder ignore the method:

````java
@Ignore
public String getFoo() { 
  return "foo";
}
````

### Lone getter that returns a collection

Some tools (such as [wsdl2java](https://cxf.apache.org/docs/wsdl-to-java.html)) 
can generate a getter with no corresponding setter, that looks like this:

````java
public List<Employee> getEmployees() {
  if (this.employees == null) {
    this.employees = new ArrayList<Employee>();
  }
  return this.employees;
}
````

In this case (lone getter that returns a subclass of `Collection`),
it is not necessary to ignore the getter.
Zerobuilder will assume that the collection is mutable and generate the correct builder code.
