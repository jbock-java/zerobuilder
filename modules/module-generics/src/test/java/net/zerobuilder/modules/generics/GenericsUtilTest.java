package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeVariableName;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static net.zerobuilder.modules.generics.GenericsUtil.references;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class GenericsUtilTest {

  private static final TypeVariableName K = TypeVariableName.get("K");
  private static final TypeVariableName V = TypeVariableName.get("V");

  private static final ParameterizedTypeName LIST_OF_K =
      ParameterizedTypeName.get(ClassName.get(List.class), K);
  private static final ParameterizedTypeName LIST_OF_V =
      ParameterizedTypeName.get(ClassName.get(List.class), V);
  private static final ParameterizedTypeName MAP_K_V =
      ParameterizedTypeName.get(ClassName.get(Map.class), K, V);
  private static final ParameterizedTypeName MAP_V_LIST_OF_K =
      ParameterizedTypeName.get(ClassName.get(Map.class), V, LIST_OF_K);
  private static final ParameterizedTypeName MAP_K_LIST_V =
      ParameterizedTypeName.get(ClassName.get(Map.class), K, LIST_OF_V);


  @Test
  public void testReferences() throws Exception {
    assertTrue(references(LIST_OF_K, K));
    assertFalse(references(LIST_OF_K, V));
    assertTrue(references(MAP_K_V, V));
    assertTrue(references(MAP_V_LIST_OF_K, K));
  }

  @Test
  public void testReferencesS_V() throws Exception {
    TypeVariableName s = TypeVariableName.get("S", String.class);
    TypeVariableName v = TypeVariableName.get("V", s);
    assertTrue(references(v, s));
  }

  @Test
  public void testReferencesS_V_K() throws Exception {
    TypeVariableName s = TypeVariableName.get("S", String.class);
    TypeVariableName v = TypeVariableName.get("V", s);
    TypeVariableName k = TypeVariableName.get("K", v);
    assertTrue(references(k, s));
  }
}