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
import static net.zerobuilder.modules.generics.VarLife.implTypeParams;
import static net.zerobuilder.modules.generics.VarLife.methodParams;
import static net.zerobuilder.modules.generics.VarLife.typeParams;
import static net.zerobuilder.modules.generics.VarLife.varLifes;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class VarLifeTest {

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
    List<List<TypeVariableName>> life = varLifes(asList(K, V), asList(listOf(K), V, map(K, V)));
    assertThat(life, is(asList(singletonList(K), asList(K, V), asList(K, V))));
    assertThat(methodParams(life), is(asList(singletonList(K), singletonList(V))));
    assertThat(typeParams(life), is(asList(emptyList(), singletonList(K))));
    assertThat(implTypeParams(life), is(asList(emptyList(), singletonList(K))));
  }

  @Test
  public void test_MKV_K_V_LV() {
    List<List<TypeVariableName>> life = varLifes(asList(K, V), asList(map(K, V), K, V, listOf(V)));
    assertThat(life, is(asList(asList(K, V), asList(K, V), singletonList(V), singletonList(V))));
    assertThat(methodParams(life), is(asList(asList(K, V), emptyList(), emptyList())));
    assertThat(typeParams(life), is(asList(emptyList(), asList(K, V), singletonList(V))));
    assertThat(implTypeParams(life), is(asList(emptyList(), asList(K, V), asList(K, V))));
  }

  @Test
  public void test_K_V_LK_V() {
    List<List<TypeVariableName>> life = varLifes(asList(K, V), asList(K, V, listOf(K), V));
    assertThat(life, is(asList(singletonList(K), asList(K, V), asList(K, V), singletonList(V))));
    assertThat(methodParams(life), is(asList(singletonList(K), singletonList(V), emptyList())));
    assertThat(typeParams(life), is(asList(emptyList(), singletonList(K), asList(V, K))));
  }
}