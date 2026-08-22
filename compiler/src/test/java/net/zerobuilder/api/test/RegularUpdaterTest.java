package net.zerobuilder.api.test;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeSpec;
import java.io.IOException;
import java.util.List;
import javax.lang.model.element.Modifier;
import net.zerobuilder.compiler.generate.DtoContext;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.ProjectedGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.Generator;
import net.zerobuilder.modules.updater.RegularUpdater;
import org.junit.jupiter.api.Test;

import static net.zerobuilder.compiler.generate.Access.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoContext.createContext;
import static net.zerobuilder.compiler.generate.DtoProjectionInfo.createGetterMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    DtoContext.GoalContext goalContext = createContext(TYPE, GENERATED_TYPE);

    String goalName = "myGoal";
    AbstractRegularDetails details = AbstractRegularDetails.create(
        TYPE, goalName, List.of("foo"),
        PUBLIC,
        List.of());

    // use ProjectedParameter because the updater module requires projections
    ProjectedParameter fooParameter = DtoRegularParameter.create("foo", STRING,
        createGetterMethod("getFoo", List.of(IO_EXCEPTION)));
    ProjectedRegularGoalDescription description = ProjectedRegularGoalDescription.create(
        details,
        List.of(),
        List.of(fooParameter),
        goalContext);

    // Invoke the generator
    GeneratorOutput generatorOutput = Generator.generate(
        List.of(new ProjectedGoalInput(REGULAR_UPDATER_MODULE, description)));

    assertEquals(1, generatorOutput.methods().size());
    assertEquals(goalName, generatorOutput.methods().getFirst().name());
    assertEquals("myGoalUpdater", generatorOutput.methods().getFirst().method().name());
    assertEquals(1, generatorOutput.methods().getFirst().method().parameters().size());
    assertEquals(List.of(IO_EXCEPTION), generatorOutput.methods().getFirst().method().exceptions());
    assertTrue(generatorOutput.methods().getFirst().method().modifiers().contains(Modifier.STATIC));
    assertEquals(GENERATED_TYPE.nestedClass("MyGoalUpdater"),
        generatorOutput.methods().getFirst().method().returnType());

    // Get the definition of the generated type
    TypeSpec typeSpec = generatorOutput.typeSpec();
    assertEquals("MyTypeBuilders", typeSpec.name());
    assertEquals(2, typeSpec.methodSpecs().size()); // myGoalToBuilder, constructor
  }
}
