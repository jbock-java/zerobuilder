package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.zerobuilder.modules.generics.VarLife.dependents;
import static net.zerobuilder.modules.generics.VarLife.expand;
import static net.zerobuilder.modules.generics.VarLife.implTypeParams;
import static net.zerobuilder.modules.generics.VarLife.methodParams;
import static net.zerobuilder.modules.generics.VarLife.typeParams;
import static net.zerobuilder.modules.generics.VarLife.varLifes;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class VarLifeTest {

  private static final TypeVariableName S = TypeVariableName.get("S");
  private static final TypeVariableName K = TypeVariableName.get("K");
  private static final TypeVariableName V = TypeVariableName.get("V");

  private static TypeName listOf(TypeName v) {
    return ParameterizedTypeName.get(ClassName.get(List.class), v);
  }

  private static TypeName map(TypeName k, TypeName v) {
    return ParameterizedTypeName.get(ClassName.get(Map.class), k, v);
  }

  @Test
  public void test_LK_V_MKV() {
    List<List<TypeVariableName>> life = varLifes(asList(K, V), asList(listOf(K), V, map(K, V)), emptyList());
    assertThat(life, is(asList(singletonList(K), asList(K, V), asList(K, V))));
    assertThat(methodParams(life, emptyList()), is(asList(singletonList(K), singletonList(V))));
    assertThat(typeParams(life, emptyList()), is(asList(emptyList(), singletonList(K))));
    assertThat(implTypeParams(life, emptyList()), is(asList(emptyList(), singletonList(K))));
  }

  @Test
  public void test_MKV_K_V_LV() {
    List<List<TypeVariableName>> life = varLifes(asList(K, V), asList(map(K, V), K, V, listOf(V)), emptyList());
    assertThat(life, is(asList(asList(K, V), asList(K, V), singletonList(V), singletonList(V))));
    assertThat(methodParams(life, emptyList()), is(asList(asList(K, V), emptyList(), emptyList())));
    assertThat(typeParams(life, emptyList()), is(asList(emptyList(), asList(K, V), singletonList(V))));
    assertThat(implTypeParams(life, emptyList()), is(asList(emptyList(), asList(K, V), asList(K, V))));
  }

  @Test
  public void test_K_V_LK_V() {
    List<List<TypeVariableName>> life = varLifes(asList(K, V), asList(K, V, listOf(K), V), emptyList());
    assertThat(life, is(asList(singletonList(K), asList(K, V), asList(K, V), singletonList(V))));
    assertThat(methodParams(life, emptyList()), is(asList(singletonList(K), singletonList(V), emptyList())));
    assertThat(typeParams(life, emptyList()), is(asList(emptyList(), singletonList(K), asList(V, K))));
  }

  @Test
  public void testExtend() {
    TypeVariableName s = TypeVariableName.get("S", String.class);
    TypeVariableName k = TypeVariableName.get("K");
    TypeVariableName v = TypeVariableName.get("V", s);
    List<TypeVariableName> expand = expand(singletonList(v));
    assertThat(expand, is(asList(v, s)));
    List<List<TypeVariableName>> life = varLifes(asList(s, k, v), asList(k, v), emptyList());
    assertThat(life, is(asList(singletonList(k), asList(s, v))));
  }

  @Test
  public void testGenericInstance() {
    List<TypeName> parameters = asList(S, K, V, map(K, V));
    List<TypeVariableName> dependents = dependents(singletonList(S), parameters);
    List<List<TypeVariableName>> life = varLifes(asList(S, K, V), parameters, dependents);
    assertThat(typeParams(life, dependents), is(asList(singletonList(S), asList(K, V), asList(K, V))));
    assertThat(methodParams(life, dependents), is(asList(asList(K, V), emptyList(), emptyList())));
    assertThat(implTypeParams(life, dependents), is(asList(singletonList(S), asList(S, K, V), asList(S, K, V))));
  }

  @Test
  public void testDependentsS_V() {
    TypeVariableName S = TypeVariableName.get("S", String.class);
    TypeVariableName V = TypeVariableName.get("V", S);
    assertThat(dependents(singletonList(S), asList(S, V)), is(asList(S, V)));
  }

  @Test
  public void testDependentsS_K_V() {
    TypeVariableName S = TypeVariableName.get("S", String.class);
    TypeVariableName V = TypeVariableName.get("V", S);
    List<TypeVariableName> dependents = dependents(
        singletonList(S),
        asList(K, V, map(K, V)));
    assertThat(dependents, is(asList(S, V)));
  }

  @Test
  public void testInstance() {
    TypeVariableName S = TypeVariableName.get("S", String.class);
    TypeVariableName V = TypeVariableName.get("V", S);
    List<TypeVariableName> typeParameters = asList(S, K, V);
    List<TypeName> parameters = asList(K, V, map(K, V));
    List<TypeVariableName> dependents = dependents(singletonList(S), parameters);
    List<List<TypeVariableName>> life = varLifes(typeParameters, parameters, dependents);
    assertThat(life, is(asList(asList(S, K, V), asList(S, K, V), asList(S, K, V))));
    assertThat(typeParams(life, dependents), is(asList(asList(S, V), asList(K, S, V))));
    assertThat(implTypeParams(life, dependents), is(asList(asList(S, V), asList(S, V, K))));
    assertThat(methodParams(life, dependents), is(asList(singletonList(K), emptyList())));
  }
}