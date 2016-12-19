package net.zerobuilder.modules.generics;

import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static net.zerobuilder.compiler.generate.ZeroUtil.cons;
import static net.zerobuilder.compiler.generate.ZeroUtil.references;
import static net.zerobuilder.compiler.generate.ZeroUtil.reverse;

final class VarLife {

  private final List<TypeName> steps;
  private final List<TypeVariableName> typeParameters;
  private final boolean instance;

  private VarLife(List<TypeName> steps, List<TypeVariableName> typeParameters, boolean instance) {
    this.steps = steps;
    this.typeParameters = typeParameters;
    this.instance = instance;
  }

  private static final Supplier<Stream<List<TypeVariableName>>> emptyLists =
      () -> Stream.generate(ArrayList::new);

  private static List<List<TypeVariableName>> emptyLists(int n) {
    List<List<TypeVariableName>> builder = new ArrayList<>(n);
    emptyLists.get().limit(n).forEach(builder::add);
    return builder;
  }

  private <E> List<E> chop(List<E> list) {
    if (!instance) {
      return list;
    }
    return list.subList(1, list.size());
  }

  List<List<TypeVariableName>> methodParams() {
    List<List<TypeVariableName>> varLifes = varLifes(steps, typeParameters);
    List<List<TypeVariableName>> builder = emptyLists(varLifes.size() - 1);
    builder.get(0).addAll(varLifes.get(0));
    for (int i = 1; i < varLifes.size() - 1; i++) {
      for (TypeVariableName t : varLifes.get(i)) {
        if (!varLifes.get(i - 1).contains(t)) {
          builder.get(i).add(t);
        }
      }
    }
    return chop(builder);
  }

  List<List<TypeVariableName>> typeParams() {
    List<List<TypeVariableName>> varLifes = accLife(steps, typeParameters);
    varLifes = varLifes.subList(0, varLifes.size() - 2);
    return instance ? varLifes : cons(emptyList(), varLifes);
  }

  /**
   * @param typeParameters type parameters
   * @param steps          parameter types; followed by return type;
   *                       if {@code instance}, preceded by instance type
   * @param instance       is this an instance method
   * @return helper object
   */
  static VarLife create(List<TypeVariableName> typeParameters,
                        List<TypeName> steps,
                        boolean instance) {
    return new VarLife(steps, typeParameters, instance);
  }

  private static List<List<TypeVariableName>> varLifes(List<TypeName> steps,
                                                       List<TypeVariableName> typeParameters) {
    List<List<TypeVariableName>> inc = accLife(steps, typeParameters);
    List<List<TypeVariableName>> dec = reverse(accLife(reverse(steps), typeParameters));
    List<List<TypeVariableName>> builder = emptyLists(steps.size());
    for (int i = 0; i < builder.size(); i++) {
      for (TypeVariableName t : typeParameters) {
        if (inc.get(i).contains(t) && dec.get(i).contains(t)) {
          builder.get(i).add(t);
        }
      }
    }
    return builder;
  }

  private static List<List<TypeVariableName>> accLife(List<TypeName> steps,
                                                      List<TypeVariableName> typeParameters) {
    List<List<TypeVariableName>> builder = emptyLists(steps.size());
    for (TypeVariableName typeParameter : typeParameters) {
      int start = varLifeStart(typeParameter, steps);
      if (start >= 0) {
        for (int i = start; i < steps.size(); i++) {
          builder.get(i).add(typeParameter);
        }
      }
    }
    return builder;
  }

  private static int varLifeStart(TypeVariableName typeParameter, List<TypeName> steps) {
    for (int i = 0; i < steps.size(); i++) {
      TypeName step = steps.get(i);
      if (references(step, typeParameter)) {
        return i;
      }
    }
    return -1;
  }
}
