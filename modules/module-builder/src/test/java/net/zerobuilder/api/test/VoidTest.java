package net.zerobuilder.api.test;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoContext;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.RegularSimpleDescriptionInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.GeneratorInput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;
import net.zerobuilder.compiler.generate.Generator;
import net.zerobuilder.modules.builder.RegularBuilder;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static net.zerobuilder.Access.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoContext.createContext;
import static net.zerobuilder.compiler.generate.NullPolicy.ALLOW;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class VoidTest {

  private static final ClassName STRING = ClassName.get(String.class);
  private static final ClassName IO_EXCEPTION = ClassName.get(IOException.class);

  private static final RegularBuilder MODULE_BUILDER = new RegularBuilder();

  /**
   * <p>The goal method is &quot;doUpdate&quot;, see below.
   * </p>
   * <pre><code>
   *   class Throw {
   *     static void doUpdate(String message) throws IOException {
   *       throw new IOException(message);
   *     }
   *   }
   * </pre></code>
   */
  @Test
  public void test() {

    // create goal context
    DtoContext.GoalContext goalContext = createContext(
        ClassName.get(VoidTest.class).peerClass("Throw"),
        ClassName.get(VoidTest.class).nestedClass("ThrowBuilders"));

    // create goal details
    String goalName = "Void";
    StaticMethodGoalDetails details = StaticMethodGoalDetails.create(
        TypeName.VOID, goalName, singletonList("message"),
        "doUpdate", PRIVATE, emptyList(), NEW_INSTANCE);

    SimpleParameter parameter = DtoRegularParameter.create("message", STRING, ALLOW);
    SimpleRegularGoalDescription description = SimpleRegularGoalDescription.create(
        details, singletonList(IO_EXCEPTION),
        singletonList(parameter),
        goalContext);

    GeneratorInput input = GeneratorInput.create(
        goalContext, singletonList(new RegularSimpleDescriptionInput(MODULE_BUILDER, description)));

    // Invoke the generator
    GeneratorOutput output = Generator.generate(input);

    assertThat(output.methods().size(), is(1));
    assertThat(output.methods().get(0).name(), is(goalName));
    MethodSpec method = output.methods().get(0).method();
    assertThat(method.name, is("VoidBuilder"));
    assertThat(method.parameters.size(), is(0));
    assertThat(method.exceptions.size(), is(0));
    assertThat(output.nestedTypes().size(), is(2));
    Map<String, List<TypeSpec>> specs = output.nestedTypes().stream().collect(groupingBy(type -> type.name));
    System.out.println(specs.keySet());
    assertThat(specs.get("VoidBuilderImpl").size(), is(1));
    assertThat(specs.get("VoidBuilderImpl").get(0).methodSpecs.size(), is(2));
    MethodSpec messageMethod = specs.get("VoidBuilderImpl").get(0).methodSpecs.get(1);
    assertThat(messageMethod.name, is("message"));
    assertThat(messageMethod.exceptions.size(), is(1));
    assertThat(messageMethod.exceptions.get(0), is(IO_EXCEPTION));
  }
}