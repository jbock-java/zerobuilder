package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.api.test.GenericsBuilderTest;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.zerobuilder.Access.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoContext.createContext;
import static net.zerobuilder.compiler.generate.NullPolicy.ALLOW;
import static net.zerobuilder.modules.generics.VarLifeTest.listOf;
import static net.zerobuilder.modules.generics.VarLifeTest.map;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GenericsGeneratorTest {

  private static final ClassName TYPE = ClassName.get(GenericsBuilderTest.class)
      .peerClass("MyType");

  // the type we wish to generate; in this case, a nested type
  private static final ClassName GENERATED_TYPE = ClassName.get(GenericsBuilderTest.class)
      .nestedClass("MyTypeBuilders");

  private static final TypeVariableName K = TypeVariableName.get("K");
  private static final TypeVariableName V = TypeVariableName.get("V");

  private static final TypeName LIST_OF_K = listOf(K);
  private static final TypeName MAP_K_V = map(K, V);

  @Test
  public void create() throws Exception {
    GoalContext goalContext = createContext(
        TYPE, // type that contains the goal method
        GENERATED_TYPE // the type we wish to generate; it will contain all the generated code
    );

    String goalName = "multiKey"; // free choice, but should be a valid java identifier
    StaticMethodGoalDetails details = StaticMethodGoalDetails.create(
        MAP_K_V, // return type of the goal method
        goalName,
        asList("keys", "value"),
        "multiKey",
        PRIVATE,
        asList(K, V),
        NEW_INSTANCE);

    SimpleParameter keysParameter = DtoRegularParameter.create("keys", LIST_OF_K, ALLOW);
    SimpleParameter valueParameter = DtoRegularParameter.create("value", V, ALLOW);

    SimpleRegularGoalDescription description = SimpleRegularGoalDescription.create(details, emptyList(),
        asList(keysParameter, valueParameter), goalContext);

    GenericsGenerator generator = GenericsGenerator.create(description);
    assertThat(generator.methodParams, is(asList(singletonList(K), singletonList(V))));
    assertThat(generator.implTypeParams, is(asList(emptyList(), singletonList(K))));
  }
}