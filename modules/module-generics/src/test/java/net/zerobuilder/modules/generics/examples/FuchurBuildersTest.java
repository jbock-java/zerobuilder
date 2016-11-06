package net.zerobuilder.modules.generics.examples;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import org.junit.Test;

import javax.lang.model.element.TypeParameterElement;
import java.util.Map;

import static java.util.Arrays.asList;
import static net.zerobuilder.modules.generics.examples.FuchurBuilders.multiKeyBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class FuchurBuildersTest {

  @Test
  public void makeType() {
//    <K,V>(java.util.List<K>,V)java.util.Map<K,V>
    TypeVariableName k = TypeVariableName.get("K");
    MethodSpec.Builder multiKey = MethodSpec.methodBuilder("multiKey")
        .returns(ParameterizedTypeName.get(ClassName.get(Map.class), k));
    System.out.println(multiKey.build());
  }

  @Test
  public void multi() throws Exception {
    Map<String, Integer> m = multiKeyBuilder()
        .keys(asList("1", "2"))
        .value(2);
    assertThat(m.size(), is(2));
    assertThat(m.get("1"), is(2));
    assertThat(m.get("2"), is(2));
  }
}