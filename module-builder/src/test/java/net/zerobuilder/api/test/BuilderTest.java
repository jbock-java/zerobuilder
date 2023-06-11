package net.zerobuilder.api.test;

import io.jbock.javapoet.ClassName;
import io.jbock.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoContext;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.RegularSimpleGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;
import net.zerobuilder.compiler.generate.Generator;
import net.zerobuilder.modules.builder.RegularBuilder;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Modifier;
import java.util.Collections;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.zerobuilder.compiler.generate.Access.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoContext.createContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BuilderTest {

  private static final ClassName STRING = ClassName.get(String.class);
  private static final ClassName INTEGER = ClassName.get(Integer.class);

  // "goal type", see below
  private static final ClassName TYPE = ClassName.get(BuilderTest.class)
      .peerClass("MyType");

  // the type we wish to generate; in this case, a nested type
  private static final ClassName GENERATED_TYPE = ClassName.get(BuilderTest.class)
      .nestedClass("MyTypeBuilders");
  private static final RegularBuilder MODULE_BUILDER = new RegularBuilder();

  /**
   * <p>We want to generate a builder for {@code MyType#create(String, Integer)}
   * </p>
   * <pre><code>
   *   class MyType {
   *     // our goal method
   *     static MyType create(String foo, Integer bar) {
   *       return null;
   *     }
   *   }
   * </pre></code>
   */
  @Test
  public void staticMethodGoal() {

    // create goal context
    DtoContext.GoalContext goalContext = createContext(
        TYPE, // type that contains the goal method
        GENERATED_TYPE // the type we wish to generate; it will contain all the generated code
    );

    // create goal details
    String goalName = "myGoal";
    StaticMethodGoalDetails details = StaticMethodGoalDetails.create(
        TYPE, // return type of the goal method
        // names of generated classes and methods are based on this
        goalName,
        // parameter names in correct order
        asList("foo", "bar"),
        "create", // correct goal method name
        PRIVATE,
        emptyList(),
        NEW_INSTANCE);

    // use SimpleParameter because the builder module doesn't need projections
    SimpleParameter fooParameter = DtoRegularParameter.create("foo", STRING);
    SimpleParameter barParameter = DtoRegularParameter.create("bar", INTEGER);
    SimpleRegularGoalDescription description = SimpleRegularGoalDescription.create(
        details,
        Collections.emptyList(), // the goal method declares no exceptions
        // step order; not necessarily the order of the goal parameters
        asList(fooParameter, barParameter),
        goalContext);

    // Invoke the generator
    GeneratorOutput generatorOutput = Generator.generate(
        singletonList(new RegularSimpleGoalInput(MODULE_BUILDER, description)));

    assertEquals(1, generatorOutput.methods().size());
    assertEquals(goalName, generatorOutput.methods().get(0).name());
    assertEquals("myGoalBuilder", generatorOutput.methods().get(0).method().name);
    assertEquals(0, generatorOutput.methods().get(0).method().parameters.size());
    assertTrue(generatorOutput.methods().get(0).method().modifiers.contains(Modifier.STATIC));
    assertTrue(generatorOutput.methods().get(0).method().modifiers.contains(Modifier.PRIVATE));
    assertEquals(GENERATED_TYPE.nestedClass("MyGoalBuilder")
        .nestedClass("Foo"), generatorOutput.methods().get(0).method().returnType);

    // Prints nicely
    TypeSpec typeSpec = generatorOutput.typeSpec();
    assertEquals("MyTypeBuilders", typeSpec.name);
    assertEquals(2, typeSpec.methodSpecs.size()); // myGoalBuilder, constructor
  }
}
