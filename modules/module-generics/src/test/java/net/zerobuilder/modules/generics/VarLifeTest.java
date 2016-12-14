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
import static net.zerobuilder.modules.generics.VarLife.create;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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
    assertThat(life.varLifes, is(asList(singletonList(K), asList(K, V), asList(K, V))));
    assertThat(life.methodParams(), is(asList(singletonList(K), singletonList(V))));
    assertThat(life.typeParams(), is(asList(emptyList(), singletonList(K))));
    assertThat(life.implTypeParams(), is(asList(emptyList(), singletonList(K))));
  }

  @Test
  public void test_MKV_K_V_LV() {
    List<TypeVariableName> typeParameters = asList(K, V);
    VarLife life = create(typeParameters, asList(map(K, V), K, V, listOf(V)), false);
    assertThat(life.varLifes, is(asList(asList(K, V), asList(K, V), singletonList(V), singletonList(V))));
    assertThat(life.methodParams(), is(asList(asList(K, V), emptyList(), emptyList())));
    assertThat(life.typeParams(), is(asList(emptyList(), asList(K, V), singletonList(V))));
    assertThat(life.implTypeParams(), is(asList(emptyList(), asList(K, V), asList(K, V))));
  }

  @Test
  public void test_K_V_LK_V() {
    List<TypeVariableName> typeParameters = asList(K, V);
    VarLife life = create(typeParameters, asList(K, V, listOf(K), V), false);
    assertThat(life.varLifes, is(asList(singletonList(K), asList(K, V), asList(K, V), singletonList(V))));
    assertThat(life.methodParams(), is(asList(singletonList(K), singletonList(V), emptyList())));
    assertThat(life.typeParams(), is(asList(emptyList(), singletonList(K), asList(K, V))));
  }

  @Test
  public void testExtend() {
    TypeVariableName s = TypeVariableName.get("S", String.class);
    TypeVariableName k = TypeVariableName.get("K");
    TypeVariableName v = TypeVariableName.get("V", s);
    VarLife life = create(asList(s, k, v), asList(k, v), false);
    assertThat(life.varLifes, is(asList(singletonList(k), asList(s, v))));
  }

  @Test
  public void testStatic() {
    List<TypeVariableName> typeParameters = asList(S, K, V);
    List<TypeName> parameters = asList(S, K, V, map(K, V));
    VarLife life = create(typeParameters, parameters, false);
    assertThat(life.varLifes, is(asList(singletonList(S), singletonList(K), asList(K, V), asList(K, V))));
    assertThat(life.typeParams(), is(asList(emptyList(), emptyList(), singletonList(K))));
    assertThat(life.implTypeParams(), is(asList(emptyList(), singletonList(S), asList(S, K))));
    assertThat(life.methodParams(), is(asList(singletonList(S), singletonList(K), singletonList(V))));
  }

  @Test
  public void testInstance() {
    List<TypeVariableName> typeParameters = asList(S, K, V);
    List<TypeName> parameters = asList(listOf(S), S, K, V, map(K, V));
    VarLife life = create(typeParameters, parameters, true);
    assertThat(life.typeParams(), is(asList(singletonList(S), emptyList(), singletonList(K))));
    assertThat(life.implTypeParams(), is(asList(singletonList(S), singletonList(S), asList(S, K))));
    assertThat(life.methodParams(), is(asList(emptyList(), singletonList(K), singletonList(V))));
  }
}