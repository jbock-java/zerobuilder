package net.zerobuilder.compiler.random;

import com.google.common.primitives.Chars;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.Builder;
import net.zerobuilder.Updater;
import net.zerobuilder.compiler.ZeroProcessor;
import net.zerobuilder.compiler.generate.ZeroUtil;
import org.junit.Test;

import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;

public class RandomGenericsTest {

  private static final String abc =
      "ABCDEFGHIJKLMNOP";

  @Test
  public void test() {
    for (int i = 0; i < 10; i++) {
      rand();
    }
  }

  private void rand() {

    String s = "package foo;\n" +
        topLevelClass().toString();

    List<String> split = Arrays.stream(s.split("\n", -1))
        .map(line -> line + '\n')
        .collect(toList());
    JavaFileObject jfo = forSourceLines("foo.Foo", split);

    assertAbout(javaSources()).that(singletonList(jfo))
        .processedWith(new ZeroProcessor())
        .compilesWithoutError();
  }

  private List<TypeVariableName> randomVars(String in) {
    int length = ThreadLocalRandom.current().nextInt(in.length() - 1) + 1;
    return Chars.asList(in.substring(0, length).toCharArray())
        .stream()
        .map(c -> TypeVariableName.get(c.toString()))
        .collect(toList());
  }

  private List<TypeVariableName> randomExtends(List<TypeVariableName> in) {
    Random random = ThreadLocalRandom.current();
    List<TypeVariableName> builder = new ArrayList<>(in.size());
    builder.addAll(nCopies(in.size(), null));
    for (int i = 0; i < builder.size(); i++) {
      TypeVariableName var = in.get(i);
      if (i > 0 && random.nextBoolean()) {
        TypeVariableName foo = TypeVariableName.get(var.name, in.get(random.nextInt(i)));
        builder.set(i, foo);
      } else {
        builder.set(i, var);
      }
    }
    return builder;
  }

  private TypeSpec innerClass(List<TypeVariableName> inner) {
    return TypeSpec.classBuilder("Bar")
        .addTypeVariables(inner)
        .addFields(inner.stream()
            .map(type -> FieldSpec.builder(type,
                'p' + type.name.toLowerCase(), FINAL).build())
            .collect(toList()))
        .addMethod(constructorBuilder()
            .addParameters(inner.stream()
                .map(type -> ParameterSpec.builder(type,
                    'p' + type.name.toLowerCase()).build())
                .collect(toList()))
            .addCode(inner.stream()
                .map(type -> statement("this.$L = $L",
                    'p' + type.name.toLowerCase(),
                    'p' + type.name.toLowerCase()))
                .collect(ZeroUtil.joinCodeBlocks))
            .build())
        .addModifiers(STATIC, FINAL)
        .build();
  }

  private TypeSpec topLevelClass() {
    List<TypeVariableName> allVars = randomExtends(randomVars(abc));
    int split = ThreadLocalRandom.current().nextInt(allVars.size());
    List<TypeVariableName> outer = allVars.subList(0, split);
    List<TypeVariableName> inner = allVars.subList(split, allVars.size());

    ClassName generated = ClassName.get("foo", "Foo");
    return TypeSpec.classBuilder(generated)
        .addFields(outer.stream()
            .map(type -> FieldSpec.builder(type, 'p' + type.name.toLowerCase(), FINAL)
                .build())
            .collect(toList()))
        .addTypeVariables(outer)
        .addMethod(constructorBuilder()
            .addParameters(outer.stream()
                .map(type -> ParameterSpec.builder(type,
                    'p' + type.name.toLowerCase()).build())
                .collect(toList()))
            .addCode(outer.stream()
                .map(type -> statement("this.$L = $L",
                    'p' + type.name.toLowerCase(),
                    'p' + type.name.toLowerCase()))
                .collect(ZeroUtil.joinCodeBlocks))
            .build())
        .addMethod(methodBuilder("bar")
            .addAnnotation(Updater.class)
            .addAnnotation(Builder.class)
            .addTypeVariables(inner)
            .returns(ParameterizedTypeName.get(ClassName.get("", "Bar"),
                allVars.toArray(new TypeVariableName[allVars.size()])))
            .addParameters(inner.stream()
                .map(type -> ParameterSpec.builder(type, 'p' + type.name.toLowerCase()).build())
                .collect(toList()))
            .addStatement("return new $T<>($L)", ClassName.get("", "Bar"),
                allVars.stream()
                    .map(type -> 'p' + type.name.toLowerCase())
                    .collect(joining(", ")))
            .build())
        .addType(innerClass(allVars))
        .addModifiers(FINAL)
        .build();
  }
}
