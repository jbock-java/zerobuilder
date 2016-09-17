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

Just add two annotations `@Builders` and `@Goal` to a POJO class:

````java
@Builders(recycle = true)
@Goal(builder = true, toBuilder = true)
public class BusinessAnalyst {

  private String name;
  private int age;
  private List<String> notes;
  private boolean isExecutive;

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public int getAge() { return age; }
  public void setAge(int age) { this.age = age; }
  public List<String> getNotes() {
    if (notes == null) {
      notes = new ArrayList<>();
    }
    return notes;
  }
  public boolean isExecutive() { return isExecutive; }
  public void setExecutive(boolean executive) { isExecutive = executive; }
}
````

Notice that there is no setter for the `notes` property.
Generators like Apache CXF actually generate code like this.

If zerobuilder is in the classpath, a class `BusinessAnalystBuilders` will be generated.
This class has two static methods that can be used to create and "update" instances of `BusinessAnalystBuilder`.

````java
public final class BusinessAnalystBuilders {
  public static BusinessAnalystBuilder.Age businessAnalystBuilder() { ... }
  public static BusinessAnalystUpdater businessAnalystToBuilder(BusinessAnalyst businessAnalyst) { ... }
}
````

If you clone this project and do a `mvn install`, you will find the complete source code of `BusinessAnalystBuilders.java`
in the `examples/basic/target/generated-sources/annotations` folder.

To see how the generated class is used, have a look at
[BusinessAnalystTest](../master/examples/basic/src/test/java/net/zerobuilder/examples/basic/beans/more/BusinessAnalystTest.java).

### Recycle

Creating and discarding an intermediate builder object in each create and update operation 
might create more pressure on the garbage collector.

If you feel that zerobuilder affects performance,
you could try the `recycle = true` option.
This will cause builder instances to be stored in a `ThreadLocal` and reused.
