package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.api.test.GenericsBuilderTest;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.InstanceMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoMethodGoal.InstanceMethodGoalContext;
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
import static net.zerobuilder.modules.generics.VarLifeTest.map;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GenericsGeneratorInstanceTest {

  private static final ClassName TYPE = ClassName.get(GenericsBuilderTest.class)
      .peerClass("MyType");

  // the type we wish to generate; in this case, a nested type
  private static final ClassName GENERATED_TYPE = ClassName.get(GenericsBuilderTest.class)
      .nestedClass("MyTypeBuilders");

  private static final TypeVariableName S = TypeVariableName.get("S");
  private static final TypeVariableName K = TypeVariableName.get("K");
  private static final TypeVariableName V = TypeVariableName.get("V", S);

  private static final TypeName MAP_K_V = map(K, V);

  /**
   * <pre><code>
   *
   *   class Instance<S extends String> {
   *     <K, V extends S> Map<K, V> map(K key, V value) {}
   *   }
   *
   * </code></pre>
   *
   * @throws Exception
   */
  @Test
  public void create() throws Exception {
    GoalContext goalContext = createContext(
        TYPE, // type that contains the goal method
        GENERATED_TYPE // the type we wish to generate; it will contain all the generated code
    );

    String goalName = "foo"; // free choice
    InstanceMethodGoalDetails details = InstanceMethodGoalDetails.create(
        MAP_K_V, // return type of the goal method
        goalName,
        asList("key", "value"),
        "map",
        PRIVATE,
        asList(K, V),
        singletonList(S),
        asList(K, V),
        NEW_INSTANCE);

    SimpleParameter keyParameter = DtoRegularParameter.create("key", K, ALLOW);
    SimpleParameter valueParameter = DtoRegularParameter.create("value", V, ALLOW);

    InstanceMethodGoalContext goal = new InstanceMethodGoalContext(
        goalContext,
        details,
        SimpleRegularGoalDescription.create(details, emptyList(), asList(keyParameter, valueParameter)));

    GenericsGenerator generator = GenericsGenerator.create(goal);
    assertThat(generator.methodParams, is(asList(singletonList(K), emptyList())));
    assertThat(generator.implTypeParams, is(asList(asList(S, V), asList(S, V, K))));
  }
}