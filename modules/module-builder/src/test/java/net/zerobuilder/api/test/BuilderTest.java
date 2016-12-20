package net.zerobuilder.api.test;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoContext;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.RegularSimpleGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;
import net.zerobuilder.compiler.generate.Generator;
import net.zerobuilder.modules.builder.RegularBuilder;
import org.junit.Test;

import javax.lang.model.element.Modifier;
import java.util.Collections;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.zerobuilder.compiler.generate.Access.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoContext.createContext;
import static net.zerobuilder.compiler.generate.NullPolicy.ALLOW;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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
    SimpleParameter fooParameter = DtoRegularParameter.create("foo", STRING, ALLOW);
    SimpleParameter barParameter = DtoRegularParameter.create("bar", INTEGER, ALLOW);
    SimpleRegularGoalDescription description = SimpleRegularGoalDescription.create(
        details,
        Collections.emptyList(), // the goal method declares no exceptions
        // step order; not necessarily the order of the goal parameters
        asList(fooParameter, barParameter),
        goalContext);

    // Invoke the generator
    GeneratorOutput generatorOutput = Generator.generate(
        singletonList(new RegularSimpleGoalInput(MODULE_BUILDER, description)));

    assertThat(generatorOutput.methods().size(), is(1));
    assertThat(generatorOutput.methods().get(0).name(), is(goalName));
    assertThat(generatorOutput.methods().get(0).method().name, is("myGoalBuilder"));
    assertThat(generatorOutput.methods().get(0).method().parameters.size(), is(0));
    assertThat(generatorOutput.methods().get(0).method().modifiers.contains(Modifier.STATIC), is(true));
    assertThat(generatorOutput.methods().get(0).method().modifiers.contains(Modifier.PRIVATE), is(true));
    assertThat(generatorOutput.methods().get(0).method().returnType,
        is(GENERATED_TYPE.nestedClass("MyGoalBuilder")
            .nestedClass("Foo")));

    // Prints nicely
    TypeSpec typeSpec = generatorOutput.typeSpec();
    assertThat(typeSpec.name, is("MyTypeBuilders"));
    assertThat(typeSpec.methodSpecs.size(), is(2)); // myGoalBuilder, constructor
  }
}