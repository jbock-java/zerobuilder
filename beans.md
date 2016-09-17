# Zerobuilder for beans

### Exampel: Generating the Builder

Add the two annotations `@Builders` and `@Goal` to a bean class:

````java
@Builders
@Goal(toBuilder = true)
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

Note that there is no setter for the `notes` property.
Generators like Apache CXF actually generate code like this.

If zerobuilder is in the classpath, a class `BusinessAnalystBuilders` will be generated.
This class has two static methods that can be used to create and "update" instances of `BusinessAnalystBuilder`.

````java
````

If you clone this project and do a `mvn install`, you will find the complete source code of `BusinessAnalystBuilders.java`
in the `examples/basic/target/generated-sources/annotations` folder.
