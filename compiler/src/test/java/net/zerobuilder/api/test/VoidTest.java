package net.zerobuilder.api.test;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
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

import static java.util.stream.Collectors.groupingBy;
import static net.zerobuilder.compiler.generate.Access.PRIVATE;
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
        TypeName.VOID, goalName, List.of("message"),
        "doUpdate", PRIVATE, List.of());

    SimpleParameter parameter = DtoRegularParameter.create("message", STRING);
    SimpleRegularGoalDescription description = SimpleRegularGoalDescription.create(
        details, List.of(IO_EXCEPTION),
        List.of(parameter),
        goalContext);

    // Invoke the generator
    GeneratorOutput output = Generator.generate(
        List.of(new RegularSimpleGoalInput(MODULE_BUILDER, description)));

    assertEquals(1, output.methods().size());
    assertEquals(goalName, output.methods().getFirst().name());
    MethodSpec method = output.methods().getFirst().method();
    assertEquals("VoidBuilder", method.name());
    assertEquals(0, method.parameters().size());
    assertEquals(0, method.exceptions().size());
    assertEquals(2, output.nestedTypes().size());
    Map<String, List<TypeSpec>> specs = output.nestedTypes().stream().collect(groupingBy(TypeSpec::name));
    assertEquals(1, specs.get("VoidBuilderImpl").size());
    assertEquals(2, specs.get("VoidBuilderImpl").getFirst().methodSpecs().size());
    MethodSpec messageMethod = specs.get("VoidBuilderImpl").getFirst().methodSpecs().get(1);
    assertEquals("message", messageMethod.name());
    assertEquals(1, messageMethod.exceptions().size());
    assertEquals(IO_EXCEPTION, messageMethod.exceptions().getFirst());
  }
}
