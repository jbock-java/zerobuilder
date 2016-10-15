package net.zerobuilder.api.test;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.Access;
import net.zerobuilder.compiler.generate.Builder;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoal.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.GoalOption;
import net.zerobuilder.compiler.generate.DtoGoal.GoalOptions;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDescription.RegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoParameter.RegularParameter;
import net.zerobuilder.compiler.generate.DtoProjectionInfo;
import net.zerobuilder.compiler.generate.Generator;
import net.zerobuilder.compiler.generate.GeneratorInput;
import net.zerobuilder.compiler.generate.Updater;
import org.junit.Test;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.Collections;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static net.zerobuilder.NullPolicy.ALLOW;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoContext.createBuildersContext;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class UpdaterTest {

  private static final ClassName STRING = ClassName.get(String.class);
  private static final ClassName IO_EXCEPTION = ClassName.get(IOException.class);

  // "goal type", see below
  private static final ClassName TYPE = ClassName.get(UpdaterTest.class)
      .peerClass("MyType");

  // the type we wish to generate; in this case, a nested type
  private static final ClassName GENERATED_TYPE = ClassName.get(UpdaterTest.class)
      .nestedClass("MyTypeBuilders");

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

    BuildersContext buildersContext = createBuildersContext(TYPE, GENERATED_TYPE, NEW_INSTANCE);

    String goalName = "myGoal";
    GoalOption updaterOption = GoalOption.create(Access.PUBLIC, new Updater()); // create an updater
    RegularGoalDetails details = ConstructorGoalDetails.create(
        TYPE, goalName, singletonList("foo"),
        GoalOptions.create(singletonList(updaterOption)));

    RegularParameter fooParameter = RegularParameter.create("foo", STRING, ALLOW,
        DtoProjectionInfo.method("getFoo", singletonList(IO_EXCEPTION)));
    RegularGoalDescription goalDescription = RegularGoalDescription.create(
        details,
        Collections.emptyList(),
        singletonList(fooParameter));

    // wrap it all together
    GeneratorInput generatorInput = GeneratorInput.create(
        buildersContext, singletonList(goalDescription));

    // Invoke the generator
    Generator generator = Generator.create(asList(new Updater(), new Builder()));
    GeneratorOutput generatorOutput = generator.generate(generatorInput);

    assertThat(generatorOutput.methods().size(), is(1));
    assertThat(generatorOutput.methods().get(0).name(), is(goalName));
    assertThat(generatorOutput.methods().get(0).method().name, is("myGoalUpdater"));
    assertThat(generatorOutput.methods().get(0).method().parameters.size(), is(1));
    assertThat(generatorOutput.methods().get(0).method().exceptions, is(singletonList(IO_EXCEPTION)));
    assertThat(generatorOutput.methods().get(0).method().modifiers.contains(Modifier.STATIC), is(true));
    assertThat(generatorOutput.methods().get(0).method().returnType,
        is(GENERATED_TYPE.nestedClass("MyGoalUpdater")));

    // Get the definition of the generated type
    TypeSpec typeSpec = generatorOutput.typeSpec();
    assertThat(typeSpec.name, is("MyTypeBuilders"));
    assertThat(typeSpec.methodSpecs.size(), is(2)); // myGoalToBuilder, constructor
  }
}