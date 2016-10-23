package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static java.lang.Character.isUpperCase;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

final class Utilities {

  static final class ClassNames {

    static final ClassName COLLECTION = ClassName.get(Collection.class);
    static final ClassName LIST = ClassName.get(List.class);
    static final ClassName SET = ClassName.get(Set.class);
    static final ClassName ITERABLE = ClassName.get(Iterable.class);
    static final ClassName THREAD_LOCAL = ClassName.get(ThreadLocal.class);

    private ClassNames() {
      throw new UnsupportedOperationException("no instances");
    }
  }

  private static final Set<String> reservedWords = new HashSet<>(Arrays.asList(
      "abstract", "continue", "for", "new", "switch", "assert", "default", "if", "package",
      "synchronized", "boolean", "do", "goto", "private", "this", "break", "double", "implements",
      "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum",
      "instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char", "final",
      "interface", "static", "void", "class", "finally", "long", "strictfp", "volatile", "const",
      "float", "native", "super", "while"));

  static final CodeBlock emptyCodeBlock = CodeBlock.of("");

  static String upcase(String s) {
    if (s.isEmpty() || Character.isUpperCase(s.charAt(0))) {
      return s;
    }
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  static String downcase(String s) {
    if (s.length() >= 2 && isUpperCase(s.charAt(1))) {
      return s;
    }
    String lowered = Character.toLowerCase(s.charAt(0)) + s.substring(1);
    if (reservedWords.contains(lowered)) {
      return s;
    }
    return lowered;
  }

  static CodeBlock statement(String format, Object... args) {
    return CodeBlock.builder().addStatement(format, args).build();
  }

  static ParameterSpec parameterSpec(TypeName type, String name) {
    return ParameterSpec.builder(type, name).build();
  }

  static FieldSpec fieldSpec(TypeName type, String name, Modifier... modifiers) {
    return FieldSpec.builder(type, name, modifiers).build();
  }

  static CodeBlock nullCheck(String varName, String message) {
    return CodeBlock.builder()
        .beginControlFlow("if ($N == null)", varName)
        .addStatement("throw new $T($S)", NullPointerException.class, message)
        .endControlFlow().build();
  }

  static CodeBlock nullCheck(ParameterSpec parameterSpec) {
    return nullCheck(parameterSpec.name, parameterSpec.name);
  }

  static CodeBlock nullCheck(ParameterSpec parameterSpec, String message) {
    return nullCheck(parameterSpec.name, message);
  }

  static String distinctFrom(String string, String other) {
    if (string.equals(other)) {
      return 'a' + upcase(string);
    }
    return string;
  }

  static Optional<ClassName> rawClassName(TypeName typeName) {
    if (typeName instanceof ClassName) {
      return Optional.of((ClassName) typeName);
    }
    if (typeName instanceof ParameterizedTypeName) {
      ParameterizedTypeName parameterized = (ParameterizedTypeName) typeName;
      return Optional.of(parameterized.rawType);
    }
    return Optional.empty();
  }

  static List<TypeName> typeArguments(TypeName typeName) {
    if (typeName instanceof ParameterizedTypeName) {
      ParameterizedTypeName parameterized = (ParameterizedTypeName) typeName;
      return parameterized.typeArguments;
    }
    return emptyList();
  }

  /**
   * @param typeName type
   * @return first type argument, if any
   * @throws IllegalArgumentException if type has multiple type arguments
   */
  static Optional<TypeName> onlyTypeArgument(TypeName typeName) {
    List<TypeName> types = typeArguments(typeName);
    switch (types.size()) {
      case 0:
        return Optional.empty();
      case 1:
        return Optional.of(types.get(0));
      default:
        throw new IllegalArgumentException("multiple type arguments");
    }
  }

  static <X, E> List<E> transform(Collection<X> input, Function<X, E> function) {
    return input.stream().map(function).collect(toList());
  }

  static <P> List<P> presentInstances(Optional<P> optional) {
    if (optional.isPresent()) {
      return singletonList(optional.get());
    }
    return emptyList();
  }

  static <P> List<P> reverse(List<P> list) {
    ArrayList<P> reversed = new ArrayList<>(list.size());
    reversed.addAll(list);
    Collections.reverse(reversed);
    return reversed;
  }

  static <P> List<P> concat(P first, List<P> list) {
    ArrayList<P> builder = new ArrayList<>(list.size() + 1);
    builder.add(first);
    builder.addAll(list);
    return builder;
  }

  static <P> List<P> concat(List<P> left, List<P> right) {
    ArrayList<P> builder = new ArrayList<>(left.size() + right.size());
    builder.addAll(left);
    builder.addAll(right);
    return builder;
  }

  static final Collector<CodeBlock, CodeBlock.Builder, CodeBlock> joinCodeBlocks
      = new Collector<CodeBlock, CodeBlock.Builder, CodeBlock>() {
    @Override
    public Supplier<CodeBlock.Builder> supplier() {
      return CodeBlock::builder;
    }
    @Override
    public BiConsumer<CodeBlock.Builder, CodeBlock> accumulator() {
      return (builder, block) -> builder.add(block);
    }
    @Override
    public BinaryOperator<CodeBlock.Builder> combiner() {
      return (left, right) -> {
        left.add(right.build());
        return left;
      };
    }
    @Override
    public Function<CodeBlock.Builder, CodeBlock> finisher() {
      return CodeBlock.Builder::build;
    }
    @Override
    public Set<Characteristics> characteristics() {
      return emptySet();
    }
  };

  static <E> Collector<List<E>, List<E>, List<E>> flatList() {
    return new Collector<List<E>, List<E>, List<E>>() {
      @Override
      public Supplier<List<E>> supplier() {
        return ArrayList::new;
      }
      @Override
      public BiConsumer<List<E>, List<E>> accumulator() {
        return (left, right) -> left.addAll(right);
      }
      @Override
      public BinaryOperator<List<E>> combiner() {
        return (left, right) -> {
          left.addAll(right);
          return left;
        };
      }
      @Override
      public Function<List<E>, List<E>> finisher() {
        return Function.identity();
      }
      @Override
      public Set<Characteristics> characteristics() {
        return emptySet();
      }
    };
  }

  static <E, R> Collector<E, List<E>, R> listCollector(Function<List<E>, R> finisher) {
    return new Collector<E, List<E>, R>() {

      @Override
      public Supplier<List<E>> supplier() {
        return ArrayList::new;
      }

      @Override
      public BiConsumer<List<E>, E> accumulator() {
        return (left, right) -> left.add(right);
      }

      @Override
      public BinaryOperator<List<E>> combiner() {
        return (left, right) -> {
          left.addAll(right);
          return left;
        };
      }

      @Override
      public Function<List<E>, R> finisher() {
        return finisher;
      }

      @Override
      public Set<Characteristics> characteristics() {
        return emptySet();
      }
    };
  }


  static <R> Supplier<R> memoize(Supplier<R> supplier) {
    List<R> ref = new ArrayList<>(singletonList(null));
    return () -> {
      R element = ref.get(0);
      if (element == null) {
        element = supplier.get();
        ref.set(0, element);
      }
      return element;
    };
  }

  static <R> Predicate<R> asPredicate(Function<R, Boolean> function) {
    return r -> function.apply(r);
  }

  static MethodSpec constructor(Modifier... modifiers) {
    return constructorBuilder().addModifiers(modifiers).build();
  }

  private Utilities() {
    throw new UnsupportedOperationException("no instances");
  }
}
