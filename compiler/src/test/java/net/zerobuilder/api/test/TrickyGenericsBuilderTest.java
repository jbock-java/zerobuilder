package net.zerobuilder.api.test;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoContext;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.RegularSimpleGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;
import net.zerobuilder.compiler.generate.Generator;
import net.zerobuilder.modules.generics.GenericsBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static net.zerobuilder.compiler.generate.Access.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.createContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrickyGenericsBuilderTest {

  private static final ClassName TYPE = ClassName.get(TrickyGenericsBuilderTest.class)
      .peerClass("MyType");

  // the type we wish to generate; in this case, a nested type
  private static final ClassName GENERATED_TYPE = ClassName.get(TrickyGenericsBuilderTest.class)
      .nestedClass("MyTypeBuilders");

  private static final TypeVariableName K = TypeVariableName.get("K");
  private static final TypeVariableName V = TypeVariableName.get("V");

  private static final ParameterizedTypeName LIST_OF_K =
      ParameterizedTypeName.get(ClassName.get(List.class), K);
  private static final ParameterizedTypeName LIST_OF_V =
      ParameterizedTypeName.get(ClassName.get(List.class), V);
  private static final ParameterizedTypeName MAP_K_LIST_V =
      ParameterizedTypeName.get(ClassName.get(Map.class), K, LIST_OF_V);

  /**
   * <p>We want to generate a generics for {@code MyType#create(String, Integer)}
   * </p>
   * <pre><code>
   *   class MyType {
   *     // our goal method
   *     static <K, V> List<V> getList(Map<K, List<V>> source, K key, V defaultValue) {
   *       return null;
   *     }
   *   }
   * </pre></code>
   */
  @Disabled
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
        LIST_OF_V, // return type of the goal method
        goalName,
        List.of("source", "key", "defaultValue"),
        "getList",
        PRIVATE,
        List.of(K, V));

    // use SimpleParameter because the generics module doesn't need projections
    SimpleParameter fooParameter = DtoRegularParameter.create("source", MAP_K_LIST_V);
    SimpleParameter barParameter = DtoRegularParameter.create("key", K);
    SimpleParameter tarParameter = DtoRegularParameter.create("defaultValue", V);
    SimpleRegularGoalDescription description = SimpleRegularGoalDescription.create(
        details,
        List.of(), // the goal method declares no exceptions
        // step order; not necessarily the order of the goal parameters
        List.of(fooParameter, barParameter, tarParameter),
        goalContext);

    // Invoke the generator
    GeneratorOutput generatorOutput = Generator.generate(
        List.of(new RegularSimpleGoalInput(new GenericsBuilder(), description)));

    assertEquals(1, generatorOutput.methods().size());
    assertEquals(goalName, generatorOutput.methods().getFirst().name());
    assertEquals("multiKeyBuilder", generatorOutput.methods().getFirst().method().name());
    assertEquals(0, generatorOutput.methods().getFirst().method().parameters().size());
    assertTrue(generatorOutput.methods().getFirst().method().modifiers().contains(Modifier.STATIC));
    assertTrue(generatorOutput.methods().getFirst().method().modifiers().contains(Modifier.PRIVATE));
    Map<String, TypeSpec> nested = unique(generatorOutput.nestedTypes().stream().collect(groupingBy(TypeSpec::name)));
    TypeSpec contract = nested.get("MultiKeyBuilder");
    Map<String, TypeSpec> steps = unique(contract.typeSpecs().stream().collect(groupingBy(TypeSpec::name)));
    checkKeysContract(steps.get("Keys"));
    checkValueContract(steps.get("Value"));
  }

  private void checkKeysContract(TypeSpec keys) {
    assertEquals(1, keys.methodSpecs().size());
    assertEquals(0, keys.typeVariables().size());
    MethodSpec stepMethod = keys.methodSpecs().getFirst();
    assertEquals(List.of(K), stepMethod.typeVariables());
    assertEquals(LIST_OF_K, stepMethod.returnType());
  }

  private void checkValueContract(TypeSpec value) {
    assertEquals(1, value.methodSpecs().size());
    assertEquals(List.of(K), value.typeVariables());
    MethodSpec method = value.methodSpecs().getFirst();
    assertEquals(List.of(V), method.typeVariables());
    assertEquals(MAP_K_LIST_V, method.returnType());
  }

  private static <K, V> Map<K, V> unique(Map<K, List<V>> map) {
    Map<K, V> m = new HashMap<>();
    for (Map.Entry<K, List<V>> entry : map.entrySet()) {
      assertEquals(1, entry.getValue().size());
      m.put(entry.getKey(), entry.getValue().getFirst());
    }
    return m;
  }
}
