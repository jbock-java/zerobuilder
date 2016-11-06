package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeVariableName;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static net.zerobuilder.modules.generics.GenericsUtil.references;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GenericsUtilTest {

  private static final TypeVariableName K = TypeVariableName.get("K");
  private static final TypeVariableName V = TypeVariableName.get("V");

  private static final ParameterizedTypeName LIST_OF_K =
      ParameterizedTypeName.get(ClassName.get(List.class), K);
  private static final ParameterizedTypeName MAP_K_V =
      ParameterizedTypeName.get(ClassName.get(Map.class), K, V);
  private static final ParameterizedTypeName MAP_V_LIST_OF_K =
      ParameterizedTypeName.get(ClassName.get(Map.class), V, LIST_OF_K);

  @Test
  public void testReferences() throws Exception {
    assertTrue(references(LIST_OF_K, K));
    assertFalse(references(LIST_OF_K, V));
    assertTrue(references(MAP_K_V, V));
    assertTrue(references(MAP_V_LIST_OF_K, K));
  }
}