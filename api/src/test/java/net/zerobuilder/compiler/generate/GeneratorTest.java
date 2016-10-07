package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.AccessLevel;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorSuccess;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;
import net.zerobuilder.compiler.generate.DtoGoalDescription.RegularGoalDescription;
import org.junit.Test;

import javax.lang.model.element.Modifier;

import static net.zerobuilder.AccessLevel.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoBuildersContext.createBuildersContext;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class GeneratorTest {

  static final class MyType {
    static MyType goal(String parameter) {
      return null;
    }
  }

  @Test
  public void generate() throws Exception {
    // Arrange
    DtoBuildersContext.BuildersContext buildersContext = createBuildersContext(ClassName.get(MyType.class),
        ClassName.get(MyType.class).peerClass("MyTypeBuilders"), false);
    DtoGoal.MethodGoalDetails details = DtoGoal.MethodGoalDetails.create(
        ClassName.get(MyType.class), "goal", ImmutableList.of("parameter"), "goal", false, DtoGoal.GoalOptions.builder()
            .toBuilderAccess(PUBLIC).builderAccess(PUBLIC).build());
    ImmutableList<DtoParameter.RegularParameter> parameters = ImmutableList.of(
        DtoParameter.RegularParameter.create("parameter", ClassName.get(String.class), Optional.<String>absent(), false));
    ImmutableList<TypeName> thrownTypes = ImmutableList.of();
    RegularGoalDescription goalDescription = RegularGoalDescription.create(details,
        thrownTypes, parameters);
    ImmutableList<? extends GoalDescription> goals = ImmutableList.of(goalDescription);

    // Act
    GeneratorOutput generatorOutput = Generator.generate(GeneratorInput.create(buildersContext, goals));
    GeneratorSuccess generatorSuccess = getSuccess.apply(generatorOutput);
    TypeSpec typeSpec = generatorSuccess.typeSpec(ImmutableList.<AnnotationSpec>of());

    // Assert
    assertThat(generatorSuccess.methods().size(), is(1));
    assertThat(generatorSuccess.methods().get(0).name(), is("goal"));
    assertThat(generatorSuccess.methods().get(0).method().name, is("goalBuilder"));
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

}