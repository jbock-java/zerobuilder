package net.zerobuilder.modules.generics;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static net.zerobuilder.modules.generics.VarLife.create;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VarLifeTest {

  private static final TypeVariableName S = TypeVariableName.get("S");
  private static final TypeVariableName K = TypeVariableName.get("K");
  private static final TypeVariableName V = TypeVariableName.get("V");

  static TypeName listOf(TypeName v) {
    return ParameterizedTypeName.get(ClassName.get(List.class), v);
  }

  public static TypeName map(TypeName k, TypeName v) {
    return ParameterizedTypeName.get(ClassName.get(Map.class), k, v);
  }

  @Test
  public void test_LK_V_MKV() {
    List<TypeVariableName> typeParameters = List.of(K, V);
    VarLife life = create(typeParameters, List.of(listOf(K), V, map(K, V)));
    assertEquals(List.of(List.of(K), List.of(V)), life.methodParams());
    assertEquals(List.of(List.of(), List.of(K)), life.typeParams());
  }

  @Test
  public void test_MKV_K_V_LV() {
    List<TypeVariableName> typeParameters = List.of(K, V);
    VarLife life = create(typeParameters, List.of(map(K, V), K, V, listOf(V)));
    assertEquals(List.of(List.of(K, V), List.of(), List.of()), life.methodParams());
    assertEquals(List.of(List.of(), List.of(K, V), List.of(K, V)), life.typeParams());
  }

  @Test
  public void test_K_V_LK_V() {
    List<TypeVariableName> typeParameters = List.of(K, V);
    VarLife life = create(typeParameters, List.of(K, V, listOf(K), V));
    assertEquals(List.of(List.of(K), List.of(V), List.of()), life.methodParams());
    assertEquals(List.of(List.of(), List.of(K), List.of(K, V)), life.typeParams());
  }

  @Test
  public void testStatic() {
    List<TypeVariableName> typeParameters = List.of(S, K, V);
    List<TypeName> parameters = List.of(S, K, V, map(K, V));
    VarLife life = create(typeParameters, parameters);
    assertEquals(List.of(List.of(), List.of(S), List.of(S, K)), life.typeParams());
    assertEquals(List.of(List.of(S), List.of(K), List.of(V)), life.methodParams());
  }
}
