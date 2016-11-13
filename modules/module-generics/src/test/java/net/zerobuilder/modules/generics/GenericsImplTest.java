package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.junit.Test;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;
import static net.zerobuilder.modules.generics.GenericsImpl.basicInvoke;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GenericsImplTest {

  private CodeBlock invoke(List<TypeSpec> typeSpecs) {
    return basicInvoke(typeSpecs).stream().collect(joinCodeBlocks(", "));
  }

  @Test
  public void invoke1() {
    List<TypeSpec> typeSpecs = Stream.of("DefaultValue")
        .map(this::typeSpec)
        .collect(toList());
    CodeBlock block = invoke(typeSpecs);
    assertThat(block, is(CodeBlock.builder().add("defaultValue").build()));
  }

  @Test
  public void invoke2() {
    List<TypeSpec> typeSpecs = Stream.of("Key", "DefaultValue")
        .map(this::typeSpec)
        .collect(toList());
    CodeBlock block = invoke(typeSpecs);
    assertThat(block, is(CodeBlock.builder().add("key, defaultValue").build()));
  }

  @Test
  public void invoke3() {
    List<TypeSpec> typeSpecs = Stream.of("Source", "Key", "DefaultValue")
        .map(this::typeSpec)
        .collect(toList());
    CodeBlock block = invoke(typeSpecs);
    assertThat(block, is(CodeBlock.builder().add("keyImpl.source, key, defaultValue").build()));
  }

  @Test
  public void invoke4() {
    List<TypeSpec> typeSpecs = Stream.of("Foo", "Source", "Key", "DefaultValue")
        .map(this::typeSpec)
        .collect(toList());
    CodeBlock block = invoke(typeSpecs);
    assertThat(block, is(CodeBlock.builder().add("keyImpl.sourceImpl.foo, keyImpl.source, key, defaultValue").build()));
  }

  private TypeSpec typeSpec(String parameter) {
    return TypeSpec.classBuilder(parameter)
        .addMethod(MethodSpec.methodBuilder(downcase(parameter))
            .addParameter(ClassName.get(String.class), downcase(parameter))
            .build())
        .build();
  }
}