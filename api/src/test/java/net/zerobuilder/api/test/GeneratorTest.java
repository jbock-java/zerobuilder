package net.zerobuilder.api.test;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBuildersContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoal.GoalOptions;
import net.zerobuilder.compiler.generate.DtoGoal.MethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDescription.RegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoParameter.RegularParameter;
import net.zerobuilder.compiler.generate.Generator;
import net.zerobuilder.compiler.generate.GeneratorInput;
import org.junit.Test;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static net.zerobuilder.NullPolicy.ALLOW;
import static net.zerobuilder.compiler.generate.Access.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoBuildersContext.BuilderLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoBuildersContext.createBuildersContext;
import static net.zerobuilder.compiler.generate.DtoGoal.GoalMethodType.STATIC_METHOD;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GeneratorTest {

  private static final ClassName STRING = ClassName.get(String.class);
  private static final ClassName INTEGER = ClassName.get(Integer.class);

  // "goal type", see below
  private static final ClassName TYPE = ClassName.get(GeneratorTest.class)
      .peerClass("MyType");

  // the type we wish to generate; in this case, a nested type
  private static final ClassName GENERATED_TYPE = ClassName.get(GeneratorTest.class)
      .nestedClass("MyTypeBuilders");

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
    BuildersContext buildersContext = createBuildersContext(
        TYPE, // type that contains the goal method; in this case, this is the same as the goal type
        GENERATED_TYPE, // the type we want to generate; it will contain all the generated code
        NEW_INSTANCE // forbid caching of builder instances
    );

    // create goal details
    String goalName = "myGoal"; // free choice, but should be a valid java identifier
    MethodGoalDetails details = MethodGoalDetails.create(
        TYPE, // return type of the goal method
        // names of generated classes and methods are based on this
        goalName,
        // parameter names in correct order
        Arrays.asList("foo", "bar"),
        "create", // correct goal method name
        STATIC_METHOD, // goal method is static
        GoalOptions.builder()
            .builder(true) // DO generate builder pattern
            .builderAccess(PRIVATE) // "myGoalBuilder" will be private
            .build());

    // create parameter representations
    RegularParameter fooParameter = RegularParameter.create("foo", STRING, ALLOW);
    RegularParameter barParameter = RegularParameter.create("bar", INTEGER, ALLOW);
    RegularGoalDescription goalDescription = RegularGoalDescription.create(
        details,
        Collections.emptyList(), // the goal method declares no exceptions
        // step order; not necessarily the order of the goal parameters
        Arrays.asList(fooParameter, barParameter));

    // wrap it all together
    GeneratorInput generatorInput = GeneratorInput.create(
        buildersContext, singletonList(goalDescription));

    // Invoke the generator
    GeneratorOutput generatorOutput = Generator.generate(generatorInput);

    assertThat(generatorOutput.methods().size(), is(1));
    assertThat(generatorOutput.methods().get(0).name(), is(goalName));
    assertThat(generatorOutput.methods().get(0).method().name, is("myGoalBuilder"));
    assertThat(generatorOutput.methods().get(0).method().parameters.size(), is(0));
    assertThat(generatorOutput.methods().get(0).method().modifiers.contains(Modifier.STATIC), is(true));
    assertThat(generatorOutput.methods().get(0).method().modifiers.contains(Modifier.PRIVATE), is(true));
    assertThat(generatorOutput.methods().get(0).method().returnType,
        is(GENERATED_TYPE.nestedClass("MyGoalBuilder")
            .nestedClass("Foo")));

    // Get the definition of the generated type
    TypeSpec typeSpec = generatorOutput.typeSpec();
    assertThat(typeSpec.name, is("MyTypeBuilders"));
    assertThat(typeSpec.methodSpecs.size(), is(2)); // myGoalBuilder, constructor
  }
}