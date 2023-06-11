package net.zerobuilder.api.test;

import io.jbock.javapoet.ClassName;
import io.jbock.javapoet.MethodSpec;
import io.jbock.javapoet.ParameterizedTypeName;
import io.jbock.javapoet.TypeName;
import io.jbock.javapoet.TypeSpec;
import io.jbock.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoContext;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.RegularSimpleGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;
import net.zerobuilder.compiler.generate.Generator;
import net.zerobuilder.modules.generics.GenericsBuilder;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static net.zerobuilder.compiler.generate.Access.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoContext.createContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenericsBuilderTest {

  private static final ClassName TYPE = ClassName.get(GenericsBuilderTest.class)
      .peerClass("MyType");

  // the type we wish to generate; in this case, a nested type
  private static final ClassName GENERATED_TYPE = ClassName.get(GenericsBuilderTest.class)
      .nestedClass("MyTypeBuilders");

  private static final TypeVariableName K = TypeVariableName.get("K");
  private static final TypeVariableName V = TypeVariableName.get("V");

  private static final ParameterizedTypeName LIST_OF_K =
      ParameterizedTypeName.get(ClassName.get(List.class), K);
  private static final ParameterizedTypeName MAP_K_V =
      ParameterizedTypeName.get(ClassName.get(Map.class), K, V);

  /**
   * <p>We want to generate a generics for {@code MyType#create(String, Integer)}
   * </p>
   * <pre><code>
   *   class MyType {
   *     // our goal method
   *     static <K, V> Map<K, V> multiKey(List<K> keys, V value) {
   *       return null;
   *     }
   *   }
   * </pre></code>
   */
  @Test
  public void staticMethodGoal() {

    // create goal context
    DtoContext.GoalContext goalContext = createContext(
        TYPE, // type that contains the goal method; in this case, this is the same as the goal type
        GENERATED_TYPE // the type we wish to generate; it will contain all the generated code
    );

    // create goal details
    String goalName = "multiKey"; // free choice, but should be a valid java identifier
    StaticMethodGoalDetails details = StaticMethodGoalDetails.create(
        MAP_K_V, // return type of the goal method
        goalName,
        asList("keys", "value"),
        "multiKey",
        PRIVATE,
        asList(K, V),
        NEW_INSTANCE);

    // use SimpleParameter because the generics module doesn't need projections
    SimpleParameter fooParameter = DtoRegularParameter.create("keys", LIST_OF_K);
    SimpleParameter barParameter = DtoRegularParameter.create("value", V);
    SimpleRegularGoalDescription description = SimpleRegularGoalDescription.create(
        details,
        Collections.emptyList(), // the goal method declares no exceptions
        // step order; not necessarily the order of the goal parameters
        asList(fooParameter, barParameter),
        goalContext);

    // Invoke the generator
    GeneratorOutput generatorOutput = Generator.generate(singletonList(new RegularSimpleGoalInput(
        new GenericsBuilder(),
        description)));

    assertEquals(1, generatorOutput.methods().size());
    assertEquals(goalName, generatorOutput.methods().get(0).name());
    assertEquals("multiKeyBuilder", generatorOutput.methods().get(0).method().name);
    assertEquals(0, generatorOutput.methods().get(0).method().parameters.size());
    assertTrue(generatorOutput.methods().get(0).method().modifiers.contains(Modifier.STATIC));
    assertTrue(generatorOutput.methods().get(0).method().modifiers.contains(Modifier.PRIVATE));
    Map<String, TypeSpec> nested = unique(generatorOutput.nestedTypes().stream().collect(groupingBy(type -> type.name)));
    TypeSpec contract = nested.get("MultiKeyBuilder");
    Map<String, TypeSpec> steps = unique(contract.typeSpecs.stream().collect(groupingBy(type -> type.name)));
    checkKeysContract(steps.get("Keys"));
    checkValueContract(steps.get("Value"));
  }

  private void checkKeysContract(TypeSpec keys) {
    assertEquals(2, keys.methodSpecs.size());
    assertEquals(0, keys.typeVariables.size());
    MethodSpec stepMethod = keys.methodSpecs.get(1);
    assertEquals(singletonList(K), stepMethod.typeVariables);
    TypeName returnType = stepMethod.returnType;
    assertTrue(returnType instanceof ParameterizedTypeName);
    assertEquals("Value", ((ParameterizedTypeName) returnType).rawType.simpleName());
    assertEquals(singletonList(TypeVariableName.get("K")), ((ParameterizedTypeName) returnType).typeArguments);
  }

  private void checkValueContract(TypeSpec value) {
    assertEquals(2, value.methodSpecs.size());
    assertEquals(singletonList(K), value.typeVariables);
    MethodSpec method = value.methodSpecs.get(1);
    assertEquals(singletonList(V), method.typeVariables);
    assertEquals(MAP_K_V, method.returnType);
  }

  private static <K, V> Map<K, V> unique(Map<K, List<V>> map) {
    HashMap<K, V> m = new HashMap<>();
    for (Map.Entry<K, List<V>> entry : map.entrySet()) {
      assertEquals(1, entry.getValue().size());
      m.put(entry.getKey(), entry.getValue().get(0));
    }
    return m;
  }
}
