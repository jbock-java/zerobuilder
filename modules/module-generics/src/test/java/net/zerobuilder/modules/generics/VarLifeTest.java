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
import static net.zerobuilder.modules.generics.VarLife.methodParams;
import static net.zerobuilder.modules.generics.VarLife.typeParams;
import static net.zerobuilder.modules.generics.VarLife.varLifes;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class VarLifeTest {

  private static final TypeVariableName K = TypeVariableName.get("K");
  private static final TypeVariableName V = TypeVariableName.get("V");

  private static ParameterizedTypeName listOf(TypeVariableName v) {
    return ParameterizedTypeName.get(ClassName.get(List.class), v);
  }

  private static ParameterizedTypeName map(TypeVariableName k, TypeVariableName v) {
    return ParameterizedTypeName.get(ClassName.get(Map.class), k, v);
  }

  @Test
  public void testKV() {
    List<List<TypeName>> lists = varLifes(asList(K, V), asList(listOf(K), V));
    assertThat(lists, is(asList(singletonList(K), singletonList(V))));
    assertThat(methodParams(lists), is(asList(singletonList(K), singletonList(V))));
    assertThat(typeParams(lists), is(asList(emptyList(), singletonList(K))));
  }

  @Test
  public void testMKV() {
    List<List<TypeName>> lists = varLifes(asList(K, V), asList(map(K, V), K, V));
    assertThat(lists, is(asList(asList(K, V), asList(K, V), singletonList(V))));
    assertThat(methodParams(lists), is(asList(asList(K, V), emptyList(), emptyList())));
    assertThat(typeParams(lists), is(asList(emptyList(), asList(K, V), singletonList(V))));
  }
}