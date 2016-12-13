package net.zerobuilder.modules.generics;

import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static net.zerobuilder.compiler.generate.ZeroUtil.extractTypeVars;
import static net.zerobuilder.compiler.generate.ZeroUtil.references;

final class VarLife {

  final List<List<TypeVariableName>> varLifes;
  final List<TypeVariableName> start;

  private VarLife(List<List<TypeVariableName>> varLifes, List<TypeVariableName> start) {
    this.varLifes = varLifes;
    this.start = start;
  }

  private static final Supplier<Stream<List<TypeVariableName>>> emptyLists =
      () -> Stream.generate(ArrayList::new);

  List<List<TypeVariableName>> methodParams() {
    List<List<TypeVariableName>> builder = new ArrayList<>(varLifes.size() - 1);
    emptyLists.get().limit(varLifes.size() - 1).forEach(builder::add);
    List<TypeVariableName> previous = start;
    for (int i = 0; i < varLifes.size() - 1; i++) {
      List<TypeVariableName> typeNames = varLifes.get(i);
      for (TypeVariableName typeName : typeNames) {
        if (!previous.contains(typeName)) {
          builder.get(i).add(typeName);
        }
      }
      previous = typeNames;
    }
    return builder;
  }

  List<List<TypeVariableName>> typeParams(List<TypeVariableName> typeParameters) {
    List<List<TypeVariableName>> builder = new ArrayList<>(varLifes.size() - 1);
    emptyLists.get().limit(varLifes.size() - 1).forEach(builder::add);
    List<TypeVariableName> previous = start;
    List<TypeVariableName> later = new ArrayList<>();
    for (int i = 0; i < varLifes.size() - 1; i++) {
      boolean needsSort = !later.isEmpty();
      builder.get(i).addAll(later);
      later.clear();
      for (TypeVariableName t : varLifes.get(i)) {
        if (previous.contains(t)) {
          if (!builder.get(i).contains(t)) {
            builder.get(i).add(t);
          }
        } else {
          later.add(t);
        }
      }
      if (needsSort) {
        builder.set(i, sort(builder.get(i), typeParameters));
      }
      previous = varLifes.get(i);
    }
    return builder;
  }

  private static List<TypeVariableName> sort(List<TypeVariableName> toSort, List<TypeVariableName> orig) {
    List<TypeVariableName> result = new ArrayList<>();
    for (TypeVariableName type : orig) {
      if (toSort.contains(type)) {
        result.add(type);
      }
    }
    return result;
  }

  List<List<TypeVariableName>> implTypeParams() {
    List<List<TypeVariableName>> builder = new ArrayList<>(varLifes.size() - 1);
    emptyLists.get().limit(varLifes.size() - 1).forEach(builder::add);
    List<TypeVariableName> seen = new ArrayList<>();
    seen.addAll(start);
    for (int i = 0; i < varLifes.size() - 1; i++) {
      builder.get(i).addAll(seen);
      for (TypeVariableName typeName : varLifes.get(i)) {
        if (!seen.contains(typeName)) {
          seen.add(typeName);
        }
      }
    }
    return builder;
  }

  static VarLife create(List<TypeVariableName> typeParameters,
                        List<TypeName> steps,
                        List<TypeVariableName> dependents) {
    List<List<TypeVariableName>> builder = new ArrayList<>(steps.size());
    emptyLists.get().limit(steps.size()).forEach(builder::add);
    for (TypeVariableName typeParameter : typeParameters) {
      int start = varLifeStart(typeParameter, steps, dependents);
      if (start >= 0) {
        int end = varLifeEnd(typeParameter, steps);
        for (int i = start; i <= end; i++) {
          builder.get(i).add(typeParameter);
        }
      }
    }
    return new VarLife(builder, dependents);
  }

  static List<TypeVariableName> referencingParameters(List<TypeVariableName> init,
                                                      List<TypeName> parameters,
                                                      List<TypeVariableName> ordering) {
    List<TypeVariableName> builder = new ArrayList<>();
    for (TypeName type : parameters) {
      for (TypeVariableName t : extractTypeVars(type)) {
        if (referencesAny(t, init) && !builder.contains(t) && !init.contains(t)) {
          builder.add(t);
        }
      }
    }
    return sort(builder, ordering);
  }

  private static boolean referencesAny(TypeName typeVariableName, List<TypeVariableName> type) {
    for (TypeVariableName variableName : type) {
      if (references(typeVariableName, variableName)) {
        return true;
      }
    }
    return false;
  }

  private static int varLifeStart(TypeVariableName typeParameter, List<TypeName> steps, List<TypeVariableName> dependents) {
    for (TypeVariableName type : dependents) {
      for (TypeName step : steps) {
        if (references(step, type)) {
          return 0;
        }
      }
    }
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
