package net.zerobuilder.modules.generics;

import io.jbock.javapoet.ClassName;
import io.jbock.javapoet.ParameterizedTypeName;
import io.jbock.javapoet.TypeName;
import io.jbock.javapoet.TypeVariableName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
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
    List<TypeVariableName> typeParameters = asList(K, V);
    VarLife life = create(typeParameters, asList(listOf(K), V, map(K, V)), false);
    assertEquals(asList(singletonList(K), singletonList(V)), life.methodParams());
    assertEquals(asList(emptyList(), singletonList(K)), life.typeParams());
  }

  @Test
  public void test_MKV_K_V_LV() {
    List<TypeVariableName> typeParameters = asList(K, V);
    VarLife life = create(typeParameters, asList(map(K, V), K, V, listOf(V)), false);
    assertEquals(asList(asList(K, V), emptyList(), emptyList()), life.methodParams());
    assertEquals(asList(emptyList(), asList(K, V), asList(K, V)), life.typeParams());
  }

  @Test
  public void test_K_V_LK_V() {
    List<TypeVariableName> typeParameters = asList(K, V);
    VarLife life = create(typeParameters, asList(K, V, listOf(K), V), false);
    assertEquals(asList(singletonList(K), singletonList(V), emptyList()), life.methodParams());
    assertEquals(asList(emptyList(), singletonList(K), asList(K, V)), life.typeParams());
  }

  @Test
  public void testStatic() {
    List<TypeVariableName> typeParameters = asList(S, K, V);
    List<TypeName> parameters = asList(S, K, V, map(K, V));
    VarLife life = create(typeParameters, parameters, false);
    assertEquals(asList(emptyList(), singletonList(S), asList(S, K)), life.typeParams());
    assertEquals(asList(singletonList(S), singletonList(K), singletonList(V)), life.methodParams());
  }

  @Test
  public void testInstance() {
    List<TypeVariableName> typeParameters = asList(S, K, V);
    List<TypeName> parameters = asList(listOf(S), S, K, V, map(K, V));
    VarLife life = create(typeParameters, parameters, true);
    assertEquals(asList(singletonList(S), singletonList(S), asList(S, K)), life.typeParams());
    assertEquals(asList(emptyList(), singletonList(K), singletonList(V)), life.methodParams());
  }
}
