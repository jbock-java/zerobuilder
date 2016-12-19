package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import net.zerobuilder.compiler.generate.DtoGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;
import net.zerobuilder.compiler.generate.NullPolicy;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static net.zerobuilder.compiler.generate.Access.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GenericsImplTest {

  private final ClassName contract = ClassName.get(getClass()).nestedClass("Foo");

  private CodeBlock invoke(List<SimpleParameter> typeSpecs) {
    DtoGoalDetails.StaticMethodGoalDetails details = DtoGoalDetails.StaticMethodGoalDetails.create(
        ClassName.OBJECT, // return type of the goal method
        "someName",
        typeSpecs.stream().map(p -> p.name).collect(toList()),
        "multiKey",
        PRIVATE,
        Collections.emptyList(),
        NEW_INSTANCE);
    GenericsImpl impl = new GenericsImpl(contract, DtoRegularGoalDescription.SimpleRegularGoalDescription.create(
        details, Collections.emptyList(), typeSpecs, null));
    return impl.basicInvoke().stream().collect(joinCodeBlocks(", "));
  }

  @Test
  public void invoke1() {
    List<SimpleParameter> typeSpecs = Stream.of("defaultValue")
        .map(this::typeSpec)
        .collect(toList());
    CodeBlock block = invoke(typeSpecs);
    assertThat(block, is(CodeBlock.builder().add("defaultValue").build()));
  }

  @Test
  public void invoke2() {
    List<SimpleParameter> typeSpecs = Stream.of("key", "defaultValue")
        .map(this::typeSpec)
        .collect(toList());
    CodeBlock block = invoke(typeSpecs);
    assertThat(block, is(CodeBlock.builder().add("key, defaultValue").build()));
  }

  @Test
  public void invoke3() {
    List<SimpleParameter> typeSpecs = Stream.of("source", "key", "defaultValue")
        .map(this::typeSpec)
        .collect(toList());
    CodeBlock block = invoke(typeSpecs);
    assertThat(block, is(CodeBlock.builder().add("key.source, key, defaultValue").build()));
  }

  @Test
  public void invoke4() {
    List<SimpleParameter> typeSpecs = Stream.of("foo", "source", "key", "defaultValue")
        .map(this::typeSpec)
        .collect(toList());
    CodeBlock block = invoke(typeSpecs);
    assertThat(block, is(CodeBlock.builder().add("key.source.foo, key.source, key, defaultValue").build()));
  }

  private SimpleParameter typeSpec(String parameter) {
    return DtoRegularParameter.create(parameter, ClassName.get(String.class), NullPolicy.ALLOW);
  }


}