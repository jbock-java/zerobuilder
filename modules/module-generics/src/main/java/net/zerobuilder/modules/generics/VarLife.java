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

final class VarLife {

  private final List<TypeName> steps;
  private final List<TypeVariableName> typeParameters;
  private final boolean instance;
  private final List<List<TypeVariableName>> varLifes;

  private VarLife(List<TypeName> steps, List<TypeVariableName> typeParameters, boolean instance,
                  List<List<TypeVariableName>> varLifes) {
    this.steps = steps;
    this.typeParameters = typeParameters;
    this.instance = instance;
    this.varLifes = varLifes;
  }

  private static final Supplier<Stream<List<TypeVariableName>>> emptyLists =
      () -> Stream.generate(ArrayList::new);

  private <E> List<E> chop(List<E> list) {
    if (!instance) {
      return list;
    }
    return list.subList(1, list.size());
  }

  List<List<TypeVariableName>> methodParams() {
    List<List<TypeVariableName>> builder = new ArrayList<>(varLifes.size() - 1);
    emptyLists.get().limit(varLifes.size() - 1).forEach(builder::add);
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
    List<List<TypeVariableName>> builder = new ArrayList<>(varLifes.size() - 1);
    emptyLists.get().limit(varLifes.size() - 1).forEach(builder::add);
    for (int i = 1; i < varLifes.size() - 1; i++) {
      for (TypeVariableName t : varLifes.get(i)) {
        if (varLifes.get(i - 1).contains(t)) {
          builder.get(i).add(t);
        }
      }
      builder.set(i, sort(builder.get(i)));
    }
    return chop(builder);
  }

  List<List<TypeVariableName>> implTypeParams() {
    List<List<TypeVariableName>> varLifes = incrementingVarLifes();
    varLifes = varLifes.subList(0, varLifes.size() - 2);
    return instance ? varLifes : cons(emptyList(), varLifes);
  }

  private List<TypeVariableName> sort(List<TypeVariableName> types) {
    if (types.isEmpty() || types.size() == 1) {
      return types;
    }
    List<TypeVariableName> result = new ArrayList<>();
    for (TypeVariableName type : typeParameters) {
      if (types.contains(type)) {
        result.add(type);
      }
    }
    return result;
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
    return new VarLife(steps, typeParameters, instance, varLifes(steps, typeParameters));
  }

  private static List<List<TypeVariableName>> varLifes(List<TypeName> steps,
                                                       List<TypeVariableName> typeParameters) {
    List<List<TypeVariableName>> builder = new ArrayList<>(steps.size());
    emptyLists.get().limit(steps.size()).forEach(builder::add);
    for (TypeVariableName typeParameter : typeParameters) {
      int start = varLifeStart(typeParameter, steps);
      if (start >= 0) {
        int end = varLifeEnd(typeParameter, steps);
        for (int i = start; i <= end; i++) {
          builder.get(i).add(typeParameter);
        }
      }
    }
    return builder;
  }

  private List<List<TypeVariableName>> incrementingVarLifes() {
    List<List<TypeVariableName>> builder = new ArrayList<>(steps.size());
    emptyLists.get().limit(steps.size()).forEach(builder::add);
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

  private static int varLifeEnd(TypeVariableName typeParameter, List<TypeName> steps) {
    for (int i = steps.size() - 1; i >= 0; i--) {
      TypeName step = steps.get(i);
      if (references(step, typeParameter)) {
        return i;
      }
    }
    throw new IllegalStateException(typeParameter + " not found");
  }
}
