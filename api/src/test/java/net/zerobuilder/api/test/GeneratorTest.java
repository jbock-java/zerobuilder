package net.zerobuilder.api.test;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.Access;
import net.zerobuilder.compiler.generate.DtoBuildersContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorSuccess;
import net.zerobuilder.compiler.generate.DtoGoal.GoalOptions;
import net.zerobuilder.compiler.generate.DtoGoal.MethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;
import net.zerobuilder.compiler.generate.DtoGoalDescription.RegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoParameter.RegularParameter;
import net.zerobuilder.compiler.generate.Generator;
import net.zerobuilder.compiler.generate.GeneratorInput;
import org.junit.Test;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static net.zerobuilder.compiler.generate.Access.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoBuildersContext.createBuildersContext;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class GeneratorTest {

  private static final ClassName STRING = ClassName.get(String.class);
  public static final ClassName GENERATED_TYPE = ClassName.get(MyType.class).peerClass("MyTypeBuilders");
  public static final ClassName TYPE = ClassName.get(MyType.class);

  // what we want to build
  static final class MyType {
    static MyType goal(String parameter) {
      return null;
    }
  }

  @Test
  public void generate() throws Exception {

    // Arrange
    DtoBuildersContext.BuildersContext buildersContext = createBuildersContext(TYPE, GENERATED_TYPE, false);
    String goalName = "myGoal"; // free choice
    MethodGoalDetails details = MethodGoalDetails.create(
        TYPE, goalName, Arrays.asList("parameter"), "goal", false, goalOptions());
    RegularParameter regularParameter = RegularParameter.create("parameter", STRING, false);
    RegularGoalDescription goalDescription = RegularGoalDescription.create(details,
        thrownTypes(), Arrays.asList(regularParameter));
    List<? extends GoalDescription> goals = Arrays.asList(goalDescription);

    // Act
    GeneratorOutput generatorOutput = Generator.generate(GeneratorInput.create(buildersContext, goals));
    GeneratorSuccess generatorSuccess = getSuccess.apply(generatorOutput);
    TypeSpec typeSpec = generatorSuccess.typeSpec(emptyList());

    // Assert
    assertThat(generatorSuccess.methods().size(), is(1));
    assertThat(generatorSuccess.methods().get(0).name(), is(goalName));
    assertThat(generatorSuccess.methods().get(0).method().name, is("myGoalBuilder"));
    assertThat(generatorSuccess.methods().get(0).method().parameters.size(), is(0));
    assertThat(generatorSuccess.methods().get(0).method().modifiers.contains(Modifier.STATIC), is(true));
    assertThat(generatorSuccess.methods().get(0).method().modifiers.contains(Modifier.PRIVATE), is(true));
    assertThat(typeSpec.name, is("MyTypeBuilders"));
    assertThat(typeSpec.methodSpecs.size(), is(2)); // goalBuilder, constructor
  }

  private static final Function<GeneratorOutput, GeneratorSuccess> getSuccess
      = DtoGeneratorOutput.asFunction(new DtoGeneratorOutput.GeneratorOutputCases<GeneratorSuccess>() {
    @Override
    public GeneratorSuccess success(GeneratorSuccess output) {
      return output;
    }
    @Override
    public GeneratorSuccess failure(DtoGeneratorOutput.GeneratorFailure failure) {
      fail("failure: " + failure.message());
      return null;
    }
  });

  private static List<TypeName> thrownTypes() {
    return emptyList();
  }

  private static GoalOptions goalOptions() {
    return GoalOptions.builder()
        .builder(true)
        .builderAccess(PRIVATE)
        .build();
  }
}