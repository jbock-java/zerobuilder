package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static java.lang.Character.isUpperCase;
import static java.util.Arrays.copyOf;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

public final class ZeroUtil {

  static final class ClassNames {

    static final ClassName THREAD_LOCAL = ClassName.get(ThreadLocal.class);

    private ClassNames() {
      throw new UnsupportedOperationException("no instances");
    }
  }

  private static final Set<String> reservedWords = new HashSet<>(Arrays.asList(
      "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
      "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
      "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
      "interface", "long", "native", "new", "package", "private", "protected", "public",
      "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
      "throw", "throws", "transient", "try", "void", "volatile", "while"));

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

  static String distinctFrom(String string, String other) {
    if (string.equals(other)) {
      return 'a' + upcase(string);
    }
    return string;
  }

  public static ClassName rawClassName(TypeName typeName) {
    if (typeName instanceof ClassName) {
      return (ClassName) typeName;
    }
    if (typeName instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) typeName).rawType;
    }
    throw new IllegalArgumentException("not a declared type: " + typeName);
  }

  private static List<TypeName> typeArguments(TypeName typeName) {
    if (typeName instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) typeName).typeArguments;
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

  public static <X, E> List<E> transform(Collection<? extends X> input, Function<X, E> function) {
    return input.stream().map(function).collect(toList());
  }

  public static <P> List<P> cons(P first, List<? extends P> list) {
    List<P> builder = new ArrayList<>(list.size() + 1);
    builder.add(first);
    builder.addAll(list);
    return builder;
  }

  public static <P> List<P> concat(List<? extends P> left, List<? extends P> right) {
    if (left.isEmpty()) {
      return unmodifiableList(right);
    }
    if (right.isEmpty()) {
      return unmodifiableList(left);
    }
    List<P> builder = new ArrayList<>(left.size() + right.size());
    builder.addAll(left);
    builder.addAll(right);
    return builder;
  }

  public static final Collector<CodeBlock, List<CodeBlock>, CodeBlock> joinCodeBlocks
      = joinCodeBlocks("");

  public static Collector<CodeBlock, List<CodeBlock>, CodeBlock> joinCodeBlocks(String delimiter) {
    return new Collector<CodeBlock, List<CodeBlock>, CodeBlock>() {
      @Override
      public Supplier<List<CodeBlock>> supplier() {
        return ArrayList::new;
      }

      @Override
      public BiConsumer<List<CodeBlock>, CodeBlock> accumulator() {
        return List::add;
      }

      @Override
      public BinaryOperator<List<CodeBlock>> combiner() {
        return (left, right) -> {
          left.addAll(right);
          return left;
        };
      }

      @Override
      public Function<List<CodeBlock>, CodeBlock> finisher() {
        return blocks -> {
          if (blocks.isEmpty()) {
            return emptyCodeBlock;
          }
          CodeBlock.Builder builder = CodeBlock.builder();
          for (int i = 0; i < blocks.size() - 1; i++) {
            builder.add(blocks.get(i));
            if (!delimiter.isEmpty()) {
              builder.add(delimiter);
            }
          }
          builder.add(blocks.get(blocks.size() - 1));
          return builder.build();
        };
      }

      @Override
      public Set<Characteristics> characteristics() {
        return emptySet();
      }
    };
  }

  public static <E> Collector<List<E>, List<E>, List<E>> flatList() {
    return new Collector<List<E>, List<E>, List<E>>() {
      @Override
      public Supplier<List<E>> supplier() {
        return ArrayList::new;
      }

      @Override
      public BiConsumer<List<E>, List<E>> accumulator() {
        return List::addAll;
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
        return List::add;
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

  static <E> int[] createRanking(E[] a, E[] b) {
    if (a.length != b.length) {
      throw new IllegalArgumentException("a.length != b.length");
    }
    int[] pos = new int[a.length];
    for (int i = 0; i < a.length; i++) {
      if (b[i].equals(a[i])) {
        pos[i] = i;
      } else {
        pos[i] = indexOf(b, a[i]);
      }
    }
    return pos;
  }

  private static <E> int indexOf(E[] b, E el) {
    for (int i = 0; i < b.length; i++) {
      if (b[i].equals(el)) {
        return i;
      }
    }
    throw new IllegalArgumentException("not found: " + el);
  }

  static <E> List<E> applyRanking(int[] ranking, List<E> input) {
    if (input.size() != ranking.length) {
      throw new IllegalArgumentException("input.size() != ranking.length");
    }
    List<E> result = new ArrayList<>(input.size());
    for (int i = 0; i < input.size(); i++)
      result.add(null);
    for (int i = 0; i < input.size(); i++)
      result.set(ranking[i], input.get(i));
    return result;
  }

  public static TypeName parameterizedTypeName(ClassName raw, List<TypeVariableName> typeVars) {
    if (typeVars.isEmpty()) {
      return raw;
    }
    return ParameterizedTypeName.get(raw, typeVars.toArray(new TypeVariableName[typeVars.size()]));
  }

  private static Modifier[] addModifier(Modifier modifier, Modifier[] modifiers) {
    for (Modifier m : modifiers) {
      if (m == modifier) {
        return modifiers;
      }
    }
    Modifier[] copy = copyOf(modifiers, modifiers.length + 1);
    copy[modifiers.length] = modifier;
    return copy;
  }

  public static Modifier[] modifiers(Access access, Modifier modifiers) {
    return access == Access.PUBLIC ?
        addModifier(Modifier.PUBLIC, new Modifier[]{modifiers}) :
        access == Access.PRIVATE ?
            addModifier(Modifier.PRIVATE, new Modifier[]{modifiers}) :
            new Modifier[]{modifiers};
  }

  public static Modifier[] modifiers(Access access) {
    return access == Access.PUBLIC ?
        new Modifier[]{Modifier.PUBLIC} :
        access == Access.PRIVATE ?
            new Modifier[]{Modifier.PRIVATE} :
            new Modifier[0];
  }

  private static final class TypeWalk implements Iterator<TypeName> {

    private final Stack<TypeName> stack;

    TypeWalk(TypeName type) {
      stack = new Stack<>();
      stack.push(type);
    }

    @Override
    public boolean hasNext() {
      return !stack.isEmpty();
    }

    @Override
    public TypeName next() {
      TypeName type = stack.pop();
      if (type instanceof ParameterizedTypeName) {
        ((ParameterizedTypeName) type).typeArguments.forEach(stack::push);
      }
      if (type instanceof TypeVariableName) {
        ((TypeVariableName) type).bounds.forEach(stack::push);
      }
      return type;
    }
  }

  private static boolean maybeTypevars(TypeName type) {
    if (!(type instanceof ParameterizedTypeName
        || type instanceof TypeVariableName)) {
      return false;
    }
    if (type instanceof ParameterizedTypeName) {
      for (TypeName targ : ((ParameterizedTypeName) type).typeArguments) {
        if (targ instanceof ParameterizedTypeName
            || targ instanceof TypeVariableName) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

  public static boolean references(TypeName type, TypeVariableName test) {
    if (!maybeTypevars(type)) {
      return false;
    }
    if (type instanceof TypeVariableName
        && ((TypeVariableName) type).bounds.isEmpty()) {
      return type.equals(test);
    }
    TypeWalk walk = new TypeWalk(type);
    while (walk.hasNext()) {
      if (walk.next().equals(test)) {
        return true;
      }
    }
    return false;
  }

  public static List<TypeVariableName> extractTypeVars(TypeName type) {
    if (!maybeTypevars(type)) {
      return emptyList();
    }
    List<TypeVariableName> builder = new ArrayList<>();
    TypeWalk walk = new TypeWalk(type);
    while (walk.hasNext()) {
      TypeName next = walk.next();
      if (next instanceof TypeVariableName) {
        if (!builder.contains(next)) {
          builder.add((TypeVariableName) next);
        }
      }
    }
    return builder;
  }

  public static <E> List<E> reverse(List<E> in) {
    List<E> builder = new ArrayList<>(in);
    Collections.reverse(builder);
    return builder;
  }

  private ZeroUtil() {
    throw new UnsupportedOperationException("no instances");
  }
}
