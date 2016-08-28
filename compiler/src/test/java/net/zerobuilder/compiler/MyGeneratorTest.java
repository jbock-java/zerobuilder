package net.zerobuilder.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.junit.Test;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.internal.util.collections.Sets.newSet;

public class MyGeneratorTest {

  @Test
  public void testThreadLocalField() throws Exception {
    FieldSpec fieldSpec = MyGenerator.threadLocalField(ClassName.get(String.class));
    assertThat(fieldSpec.modifiers, is(newSet(PRIVATE, STATIC, FINAL)));
    assertThat(fieldSpec.type, is((TypeName) ParameterizedTypeName.get(ThreadLocal.class, String.class)));
    assertTrue(fieldSpec.initializer.toString().contains("@java.lang.Override"));
    assertTrue(fieldSpec.initializer.toString().contains("protected java.lang.String initialValue()"));
    assertTrue(fieldSpec.initializer.toString().contains("return new java.lang.String()"));
  }

}