package net.zerobuilder.compiler.random;

import com.google.common.collect.Sets;
import com.google.common.primitives.Chars;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.concat;
import static net.zerobuilder.compiler.generate.ZeroUtil.extractTypeVars;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;

public class RandomGenericsTest {

  private static final String abc =
      "ABCD";

  @Test
  public void test() {
    for (int i = 0; i < 4; i++) {
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
    return Chars.asList(in.toCharArray())
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

  private TypeSpec innerClass(List<Parameter> allParams, List<TypeVariableName> allTypevars) {
    return TypeSpec.classBuilder("Bar")
        .addTypeVariables(allTypevars)
        .addFields(allParams.stream()
            .map(Parameter::toField)
            .collect(toList()))
        .addMethod(constructorBuilder()
            .addParameters(allParams.stream()
                .map(Parameter::toSpec)
                .collect(toList()))
            .addCode(allParams.stream()
                .map(parameter -> statement("this.$N = $N",
                    parameter.toField(),
                    parameter.toSpec()))
                .collect(ZeroUtil.joinCodeBlocks))
            .build())
        .addModifiers(STATIC, FINAL)
        .build();
  }

  private TypeSpec topLevelClass() {
    List<TypeName> allVars = mapify(randomExtends(randomVars(abc)));
    int split = allVars.size() / 2;
    List<Parameter> paramsOuter = expand(allVars.subList(0, split), 'a');
    List<Parameter> paramsInner = expand(allVars.subList(split, allVars.size()), 'b');
    List<Parameter> allParams = concat(paramsOuter, paramsInner);
    List<TypeVariableName> allTypevars = new ArrayList<>(allParams.stream()
        .map(Parameter::typevars)
        .map(List::stream)
        .flatMap(identity())
        .collect(toSet()));

    ClassName generated = ClassName.get("foo", "Foo");
    return TypeSpec.classBuilder(generated)
        .addFields(paramsOuter.stream()
            .map(Parameter::toField)
            .collect(toList()))
        .addTypeVariables(paramsOuter.stream()
            .map(Parameter::typevars)
            .map(List::stream)
            .flatMap(identity())
            .collect(toSet()))
        .addMethod(constructorBuilder()
            .addParameters(paramsOuter.stream()
                .map(Parameter::toSpec)
                .collect(toList()))
            .addCode(paramsOuter.stream()
                .map(parameter -> statement("this.$N = $N",
                    parameter.toField(),
                    parameter.toSpec()))
                .collect(ZeroUtil.joinCodeBlocks))
            .build())
        .addMethod(methodBuilder("bar")
            .addAnnotation(Updater.class)
            .addAnnotation(Builder.class)
            .addTypeVariables(Sets.difference(new HashSet<>(allTypevars), paramsOuter.stream()
                .map(Parameter::typevars)
                .map(List::stream)
                .flatMap(identity())
                .collect(toSet())))
            .returns(ParameterizedTypeName.get(ClassName.get("", "Bar"),
                allTypevars.toArray(new TypeVariableName[allTypevars.size()])))
            .addParameters(paramsInner.stream()
                .map(Parameter::toSpec)
                .collect(toList()))
            .addStatement("return new $T<>($L)", ClassName.get("", "Bar"),
                allParams.stream()
                    .map(parameter -> parameter.name)
                    .collect(joining(", ")))
            .build())
        .addType(innerClass(allParams, allTypevars))
        .addModifiers(FINAL)
        .build();
  }

  private static List<TypeName> powerize(List<TypeName> typevars) {
    Random random = ThreadLocalRandom.current();
    List<Integer> powers = Stream.generate(() -> random.nextInt(3) + 1)
        .limit(typevars.size())
        .collect(toList());
    List<TypeName> builder = new ArrayList<>(powers.stream().mapToInt(i -> i).sum());
    for (int i = 0; i < typevars.size(); i++) {
      for (int j = 0; j < powers.get(i); j++) {
        builder.add(typevars.get(i));
      }
    }
    Collections.shuffle(builder);
    return builder;
  }

  private static List<TypeName> mapify(List<? extends TypeName> types) {
    Random random = ThreadLocalRandom.current();
    List<TypeName> builder = new ArrayList<>(types.size());
    builder.add(types.get(0));
    builder.add(types.get(1));
    int pos = 2;
    for (int i = 2; i < types.size(); i++) {
      if (random.nextBoolean()) {
        List<TypeName> typeNames = pick2(builder);
        builder.add(ParameterizedTypeName.get(ClassName.get(Map.class),
            typeNames.toArray(new TypeName[typeNames.size()])));
      } else {
        builder.add(types.get(pos++));
      }
    }
    return builder;
  }


  private static List<Parameter> expand(List<TypeName> typevars, char prefix) {
    List<TypeName> builder = powerize(typevars);
    Collections.shuffle(builder);
    List<Parameter> parameters = new ArrayList<>(builder.size());
    int[] count = new int[typevars.size()];
    int mapcount = 0;
    for (TypeName type : builder) {
      if (type instanceof TypeVariableName) {
        int idx = typevars.indexOf(type);
        parameters.add(new Parameter(type, prefix + type.toString().toLowerCase() + count[idx]++));
      } else {
        parameters.add(new Parameter(type, "map" + mapcount++));
      }
    }
    return parameters;
  }

  private static final class Parameter {
    final TypeName type;
    final String name;

    Parameter(TypeName type, String name) {
      this.type = type;
      this.name = name;
    }

    @Override
    public String toString() {
      return "[" + type + ", " + name + ']';
    }

    ParameterSpec toSpec() {
      return ParameterSpec.builder(type, name).build();
    }

    FieldSpec toField() {
      return FieldSpec.builder(type, name, FINAL).build();
    }

    List<TypeVariableName> typevars() {
      return extractTypeVars(type);
    }
  }

  private static <E> List<E> pick2(List<E> in) {
    Random random = ThreadLocalRandom.current();
    int i = random.nextInt(in.size());
    int j = random.nextInt(in.size() - 1);
    if (j >= i) {
      j++;
    }
    return asList(in.get(i), in.get(j));
  }
}
