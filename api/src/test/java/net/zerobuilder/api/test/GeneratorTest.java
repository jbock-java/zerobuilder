package net.zerobuilder.api.test;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
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

import static net.zerobuilder.compiler.generate.DtoBuildersContext.createBuildersContext;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class GeneratorTest {

  private static final ClassName STRING = ClassName.get(String.class);
  public static final ClassName GENERATED_TYPE = ClassName.get(MyType.class).peerClass("MyTypeBuilders");
  public static final ClassName TYPE = ClassName.get(MyType.class);

  // a model of what we want to build, for testing convenience
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
        TYPE, goalName, ImmutableList.of("parameter"), "goal", false, goalOptions());
    RegularParameter regularParameter = RegularParameter.create("parameter", STRING, getter(), false);
    RegularGoalDescription goalDescription = RegularGoalDescription.create(details,
        thrownTypes(), ImmutableList.of(regularParameter));
    ImmutableList<? extends GoalDescription> goals = ImmutableList.of(goalDescription);

    // Act
    GeneratorOutput generatorOutput = Generator.generate(GeneratorInput.create(buildersContext, goals));
    GeneratorSuccess generatorSuccess = getSuccess.apply(generatorOutput);
    TypeSpec typeSpec = generatorSuccess.typeSpec(ImmutableList.<AnnotationSpec>of());

    // Assert
    assertThat(generatorSuccess.methods().size(), is(1));
    assertThat(generatorSuccess.methods().get(0).name(), is(goalName));
    assertThat(generatorSuccess.methods().get(0).method().name, is("myGoalBuilder"));
    assertThat(generatorSuccess.methods().get(0).method().parameters.size(), is(0));
    assertThat(generatorSuccess.methods().get(0).method().modifiers.contains(Modifier.STATIC), is(true));
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

  private static ImmutableList<TypeName> thrownTypes() {
    return ImmutableList.of();
  }

  private static GoalOptions goalOptions() {
    return GoalOptions.builder().build();
  }

  private static Optional<String> getter() {
    return Optional.absent();
  }
}