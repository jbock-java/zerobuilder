package net.zerobuilder.api.test;

import io.jbock.javapoet.ClassName;
import io.jbock.javapoet.MethodSpec;
import io.jbock.javapoet.TypeName;
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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static net.zerobuilder.compiler.generate.Access.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoContext.createContext;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    SimpleParameter parameter = DtoRegularParameter.create("message", STRING);
    SimpleRegularGoalDescription description = SimpleRegularGoalDescription.create(
        details, singletonList(IO_EXCEPTION),
        singletonList(parameter),
        goalContext);

    // Invoke the generator
    GeneratorOutput output = Generator.generate(
        singletonList(new RegularSimpleGoalInput(MODULE_BUILDER, description)));

    assertEquals(1, output.methods().size());
    assertEquals(goalName, output.methods().get(0).name());
    MethodSpec method = output.methods().get(0).method();
    assertEquals("VoidBuilder", method.name);
    assertEquals(0, method.parameters.size());
    assertEquals(0, method.exceptions.size());
    assertEquals(2, output.nestedTypes().size());
    Map<String, List<TypeSpec>> specs = output.nestedTypes().stream().collect(groupingBy(type -> type.name));
    assertEquals(1, specs.get("VoidBuilderImpl").size());
    assertEquals(2, specs.get("VoidBuilderImpl").get(0).methodSpecs.size());
    MethodSpec messageMethod = specs.get("VoidBuilderImpl").get(0).methodSpecs.get(1);
    assertEquals("message", messageMethod.name);
    assertEquals(1, messageMethod.exceptions.size());
    assertEquals(IO_EXCEPTION, messageMethod.exceptions.get(0));
  }
}
