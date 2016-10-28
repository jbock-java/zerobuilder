package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.SimpleDescriptionInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.GeneratorInput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoal.MethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;
import org.junit.Test;

import java.io.IOException;

import static java.util.Collections.singletonList;
import static net.zerobuilder.NullPolicy.ALLOW_NULL;
import static net.zerobuilder.compiler.generate.Access.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoContext.createBuildersContext;
import static net.zerobuilder.compiler.generate.DtoGoal.GoalMethodType.STATIC_METHOD;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BuilderTest {

  private static final ClassName STRING = ClassName.get(String.class);
  private static final ClassName IO_EXCEPTION = ClassName.get(IOException.class);

  private static final Builder MODULE_BUILDER = new Builder();

  /**
   * <pre><code>
   *   class Throw {
   *     static void doUpdate(String message) throws IOException {
   *       throw new IOException(message);
   *     }
   *   }
   * </pre></code>
   */
  @Test
  public void staticMethodGoal() {

    // create goal context
    BuildersContext buildersContext = createBuildersContext(
        ClassName.get(BuilderTest.class).peerClass("Throw"),
        ClassName.get(BuilderTest.class).nestedClass("ThrowBuilders"),
        NEW_INSTANCE);

    // create goal details
    String goalName = "Void"; // free choice, but should be a valid java identifier
    MethodGoalDetails details = MethodGoalDetails.create(
        TypeName.VOID, goalName, singletonList("message"),
        "doUpdate", STATIC_METHOD, PRIVATE);

    SimpleParameter fooParameter = DtoRegularParameter.create("message", STRING, ALLOW_NULL);
    SimpleRegularGoalDescription description = SimpleRegularGoalDescription.create(
        details, singletonList(IO_EXCEPTION),
        singletonList(fooParameter));

    // wrap it all together
    GeneratorInput generatorInput = GeneratorInput.create(
        buildersContext, singletonList(new SimpleDescriptionInput(MODULE_BUILDER, description)));

    // Invoke the generator
    GeneratorOutput generatorOutput = Generator.generate(generatorInput);

    assertThat(generatorOutput.methods().size(), is(1));
    assertThat(generatorOutput.methods().get(0).name(), is(goalName));
    assertThat(generatorOutput.methods().get(0).method().name, is("VoidBuilder"));
    assertThat(generatorOutput.methods().get(0).method().parameters.size(), is(0));
    assertThat(generatorOutput.methods().get(0).method().exceptions.size(), is(1));
    assertThat(generatorOutput.methods().get(0).method().exceptions.get(0), is(IO_EXCEPTION));
  }
}