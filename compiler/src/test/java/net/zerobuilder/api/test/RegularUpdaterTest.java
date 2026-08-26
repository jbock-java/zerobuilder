package net.zerobuilder.api.test;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeSpec;
import java.io.IOException;
import java.util.List;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.ProjectedParameter;
import net.zerobuilder.compiler.generate.Generator;
import net.zerobuilder.compiler.generate.GoalContext;
import net.zerobuilder.compiler.generate.GoalDescription;
import net.zerobuilder.compiler.generate.GoalDetails;
import net.zerobuilder.modules.updater.RegularUpdater;
import org.junit.jupiter.api.Test;

import static net.zerobuilder.compiler.generate.Access.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoProjectionInfo.createGetterMethod;
import static net.zerobuilder.compiler.generate.DtoRegularGoalDescription.createUpdaterGoalDescription;
import static net.zerobuilder.compiler.generate.GoalDetails.createGoalDetails;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegularUpdaterTest {

  private static final ClassName STRING = ClassName.get(String.class);
  private static final ClassName IO_EXCEPTION = ClassName.get(IOException.class);

  // "goal type", see below
  private static final ClassName TYPE = ClassName.get(RegularUpdaterTest.class)
      .peerClass("MyType");

  // the type we wish to generate; in this case, a nested type
  private static final ClassName GENERATED_TYPE = ClassName.get(RegularUpdaterTest.class)
      .nestedClass("MyTypeBuilders");
  public static final RegularUpdater REGULAR_UPDATER_MODULE = new RegularUpdater();

  /**
   * <p>We want to generate updater for the following
   * </p>
   * <pre><code>
   *   class MyType {
   *     MyType(String foo) {
   *     }
   *     String getFoo() throws IOException {
   *     }
   *   }
   * </pre></code>
   */
  @Test
  public void test() {

    GoalContext goalContext = new GoalContext(TYPE, GENERATED_TYPE);

    String goalName = "myGoal";
    GoalDetails details = createGoalDetails(
        TYPE, goalName, List.of("foo"),
        PUBLIC,
        List.of());

    // use ProjectedParameter because the updater module requires projections
    ProjectedParameter fooParameter = new ProjectedParameter("foo", STRING,
        createGetterMethod("getFoo", List.of(IO_EXCEPTION)));
    GoalDescription description = createUpdaterGoalDescription(
        details,
        List.of(),
        List.of(fooParameter),
        goalContext);

    // Invoke the generator
    GeneratorOutput generatorOutput = Generator.generate(description);

    assertEquals(2, generatorOutput.methods().size());
    assertEquals("builder", generatorOutput.methods().getFirst().name());

    // Get the definition of the generated type
    TypeSpec typeSpec = generatorOutput.typeSpec();
    assertEquals("MyTypeBuilders", typeSpec.name());
    assertEquals(3, typeSpec.methodSpecs().size()); // two builder methods, plus constructor
  }
}
