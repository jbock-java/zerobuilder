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
import static net.zerobuilder.compiler.generate.ZeroUtil.concat;
import static net.zerobuilder.compiler.generate.ZeroUtil.cons;
import static net.zerobuilder.modules.generics.VarLife.create;
import static net.zerobuilder.modules.generics.VarLife.referencingParameters;
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
    VarLife life = create(typeParameters, asList(listOf(K), V, map(K, V)), emptyList());
    assertThat(life.varLifes, is(asList(singletonList(K), asList(K, V), asList(K, V))));
    assertThat(life.methodParams(), is(asList(singletonList(K), singletonList(V))));
    assertThat(life.typeParams(typeParameters), is(asList(emptyList(), singletonList(K))));
    assertThat(life.implTypeParams(), is(asList(emptyList(), singletonList(K))));
  }

  @Test
  public void test_MKV_K_V_LV() {
    List<TypeVariableName> typeParameters = asList(K, V);
    VarLife life = create(typeParameters, asList(map(K, V), K, V, listOf(V)), emptyList());
    assertThat(life.varLifes, is(asList(asList(K, V), asList(K, V), singletonList(V), singletonList(V))));
    assertThat(life.methodParams(), is(asList(asList(K, V), emptyList(), emptyList())));
    assertThat(life.typeParams(typeParameters), is(asList(emptyList(), asList(K, V), singletonList(V))));
    assertThat(life.implTypeParams(), is(asList(emptyList(), asList(K, V), asList(K, V))));
  }

  @Test
  public void test_K_V_LK_V() {
    List<TypeVariableName> typeParameters = asList(K, V);
    VarLife life = create(typeParameters, asList(K, V, listOf(K), V), emptyList());
    assertThat(life.varLifes, is(asList(singletonList(K), asList(K, V), asList(K, V), singletonList(V))));
    assertThat(life.methodParams(), is(asList(singletonList(K), singletonList(V), emptyList())));
    assertThat(life.typeParams(typeParameters), is(asList(emptyList(), singletonList(K), asList(K, V))));
  }

  @Test
  public void testExtend() {
    TypeVariableName s = TypeVariableName.get("S", String.class);
    TypeVariableName k = TypeVariableName.get("K");
    TypeVariableName v = TypeVariableName.get("V", s);
    VarLife life = create(asList(s, k, v), asList(k, v), emptyList());
    assertThat(life.varLifes, is(asList(singletonList(k), asList(s, v))));
  }

  @Test
  public void testGenericInstance() {
    List<TypeName> parameters = asList(S, K, V, map(K, V));
    List<TypeVariableName> dependents = concat(singletonList(S),
        referencingParameters(singletonList(S), parameters, asList(K, V)));
    List<TypeVariableName> typeParameters = asList(S, K, V);
    VarLife life = create(typeParameters, parameters, dependents);
    assertThat(life.typeParams(typeParameters), is(asList(singletonList(S), asList(K, V), asList(K, V))));
    assertThat(life.methodParams(), is(asList(asList(K, V), emptyList(), emptyList())));
    assertThat(life.implTypeParams(), is(asList(singletonList(S), asList(S, K, V), asList(S, K, V))));
  }

  @Test
  public void testDependentsS_V() {
    TypeVariableName S = TypeVariableName.get("S", String.class);
    TypeVariableName V = TypeVariableName.get("V", S);
    List<TypeVariableName> ref = referencingParameters(singletonList(S), asList(S, V), asList(S, V));
    assertThat(ref, is(singletonList(V)));
  }

  @Test
  public void testDependentsS_K_V() {
    TypeVariableName S = TypeVariableName.get("S", String.class);
    TypeVariableName V = TypeVariableName.get("V", S);
    List<TypeVariableName> ref = referencingParameters(
        singletonList(S),
        asList(K, V, map(K, V)),
        asList(K, S, V));
    assertThat(ref, is(singletonList(V)));
  }

  @Test
  public void testInstance() {
    TypeVariableName S = TypeVariableName.get("S", String.class);
    TypeVariableName V = TypeVariableName.get("V", S);
    List<TypeVariableName> typeParameters = asList(S, K, V);
    List<TypeName> parameters = asList(K, V, map(K, V));
    List<TypeVariableName> dependents = cons(S, referencingParameters(singletonList(S), parameters, asList(S, V, K)));
    VarLife life = create(typeParameters, parameters, dependents);
    assertThat(life.varLifes, is(asList(asList(S, K, V), asList(S, K, V), asList(S, K, V))));
    assertThat(life.typeParams(typeParameters), is(asList(asList(S, V), asList(S, K, V))));
    assertThat(life.implTypeParams(), is(asList(asList(S, V), asList(S, V, K))));
    assertThat(life.methodParams(), is(asList(singletonList(K), emptyList())));
  }
}