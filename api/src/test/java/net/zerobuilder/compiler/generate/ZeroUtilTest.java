package net.zerobuilder.compiler.generate;

import io.jbock.javapoet.ClassName;
import io.jbock.javapoet.ParameterizedTypeName;
import io.jbock.javapoet.TypeVariableName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static net.zerobuilder.compiler.generate.ZeroUtil.extractTypeVars;
import static net.zerobuilder.compiler.generate.ZeroUtil.references;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZeroUtilTest {

  private static final TypeVariableName K = TypeVariableName.get("K");
  private static final TypeVariableName V = TypeVariableName.get("V");

  private static final ParameterizedTypeName LIST_OF_K =
      ParameterizedTypeName.get(ClassName.get(List.class), K);
  private static final ParameterizedTypeName MAP_K_V =
      ParameterizedTypeName.get(ClassName.get(Map.class), K, V);
  private static final ParameterizedTypeName MAP_V_LIST_OF_K =
      ParameterizedTypeName.get(ClassName.get(Map.class), V, LIST_OF_K);


  @Test
  public void testReferences() {
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

  @Test
  public void testExtractTypeVars() {
    TypeVariableName a = TypeVariableName.get("A");
    TypeVariableName b = TypeVariableName.get("B");
    TypeVariableName c = TypeVariableName.get("C");
    TypeVariableName d = TypeVariableName.get("D");
    ParameterizedTypeName ab = ParameterizedTypeName.get(ClassName.get(HashMap.class), a, b);
    ParameterizedTypeName abc = ParameterizedTypeName.get(ClassName.get(HashMap.class), ab, c);
    ParameterizedTypeName dabc = ParameterizedTypeName.get(ClassName.get(HashMap.class), d, abc);
    ParameterizedTypeName dabca = ParameterizedTypeName.get(ClassName.get(HashMap.class), dabc, a);
    ParameterizedTypeName dabcab = ParameterizedTypeName.get(ClassName.get(HashMap.class), dabca, b);
    ParameterizedTypeName ddabcab = ParameterizedTypeName.get(ClassName.get(HashMap.class), d, dabcab);
    List<TypeVariableName> vars = extractTypeVars(ddabcab);
    assertEquals(new HashSet<>(asList(a, b, c, d)), new HashSet<>(vars));
  }
}
