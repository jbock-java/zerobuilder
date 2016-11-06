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

public final class ZeroUtil {

  public static final class ClassNames {

    public static final ClassName COLLECTION = ClassName.get(Collection.class);
    public static final ClassName LIST = ClassName.get(List.class);
    public static final ClassName SET = ClassName.get(Set.class);
    public static final ClassName ITERABLE = ClassName.get(Iterable.class);
    public static final ClassName THREAD_LOCAL = ClassName.get(ThreadLocal.class);

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

  public static final CodeBlock emptyCodeBlock = CodeBlock.of("");

  public static String upcase(String s) {
    if (s.isEmpty() || Character.isUpperCase(s.charAt(0))) {
      return s;
    }
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  public static String downcase(String s) {
    if (s.length() >= 2 && isUpperCase(s.charAt(1))) {
      return s;
    }
    String lowered = Character.toLowerCase(s.charAt(0)) + s.substring(1);
    if (reservedWords.contains(lowered)) {
      return s;
    }
    return lowered;
  }

  public static CodeBlock statement(String format, Object... args) {
    return CodeBlock.builder().addStatement(format, args).build();
  }

  public static ParameterSpec parameterSpec(TypeName type, String name) {
    return ParameterSpec.builder(type, name).build();
  }

  public static FieldSpec fieldSpec(TypeName type, String name, Modifier... modifiers) {
    return FieldSpec.builder(type, name, modifiers).build();
  }

  public static CodeBlock nullCheck(String varName, String message) {
    return CodeBlock.builder()
        .beginControlFlow("if ($N == null)", varName)
        .addStatement("throw new $T($S)", NullPointerException.class, message)
        .endControlFlow().build();
  }

  public static CodeBlock nullCheck(ParameterSpec parameterSpec) {
    return nullCheck(parameterSpec.name, parameterSpec.name);
  }

  public static CodeBlock nullCheck(ParameterSpec parameterSpec, String message) {
    return nullCheck(parameterSpec.name, message);
  }

  public static String distinctFrom(String string, String other) {
    if (string.equals(other)) {
      return 'a' + upcase(string);
    }
    return string;
  }

  public static Optional<ClassName> rawClassName(TypeName typeName) {
    if (typeName instanceof ClassName) {
      return Optional.of((ClassName) typeName);
    }
    if (typeName instanceof ParameterizedTypeName) {
      ParameterizedTypeName parameterized = (ParameterizedTypeName) typeName;
      return Optional.of(parameterized.rawType);
    }
    return Optional.empty();
  }

  public static List<TypeName> typeArguments(TypeName typeName) {
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
  public static Optional<TypeName> onlyTypeArgument(TypeName typeName) {
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

  public static <X, E> List<E> transform(Collection<? extends X> input, Function<X, E> function) {
    return input.stream().map(function).collect(toList());
  }

  public static <P> List<P> presentInstances(Optional<P> optional) {
    if (optional.isPresent()) {
      return singletonList(optional.get());
    }
    return emptyList();
  }

  public static <P> List<P> reverse(List<P> list) {
    ArrayList<P> reversed = new ArrayList<>(list.size());
    reversed.addAll(list);
    Collections.reverse(reversed);
    return reversed;
  }

  public static <P> List<P> concat(P first, List<P> list) {
    ArrayList<P> builder = new ArrayList<>(list.size() + 1);
    builder.add(first);
    builder.addAll(list);
    return builder;
  }

  public static <P> List<P> concat(List<P> left, List<P> right) {
    ArrayList<P> builder = new ArrayList<>(left.size() + right.size());
    builder.addAll(left);
    builder.addAll(right);
    return builder;
  }

  public static final Collector<CodeBlock, CodeBlock.Builder, CodeBlock> joinCodeBlocks
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

  public static <E> Collector<List<E>, List<E>, List<E>> flatList() {
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

  public static <E, R> Collector<E, List<E>, R> listCollector(Function<List<E>, R> finisher) {
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

  public static <R> Supplier<R> memoize(Supplier<R> supplier) {
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

  public static <R> Predicate<R> asPredicate(Function<R, Boolean> function) {
    return r -> function.apply(r);
  }

  public static MethodSpec constructor(Modifier... modifiers) {
    return constructorBuilder().addModifiers(modifiers).build();
  }

  public static String simpleName(TypeName type) {
    if (type.isPrimitive() || type == TypeName.VOID) {
      return ((ClassName) type.box()).simpleName();
    }
    if (type instanceof ClassName) {
      return ((ClassName) type).simpleName();
    }
    if (type instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) type).rawType.simpleName();
    }
    throw new IllegalArgumentException("unknown kind: " + type);
  }

  private ZeroUtil() {
    throw new UnsupportedOperationException("no instances");
  }
}
