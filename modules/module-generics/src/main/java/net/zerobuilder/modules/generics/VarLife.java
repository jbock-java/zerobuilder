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

  private static final Supplier<Stream<List<TypeVariableName>>> emptyLists =
      () -> Stream.generate(ArrayList::new);

  static List<List<TypeVariableName>> methodParams(List<List<TypeVariableName>> varLifes, List<TypeVariableName> start) {
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

  static List<List<TypeVariableName>> typeParams(List<TypeVariableName> typeParameters,
                                                 List<List<TypeVariableName>> varLifes,
                                                 List<TypeVariableName> start) {
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

  static List<TypeVariableName> sort(List<TypeVariableName> toSort, List<TypeVariableName> orig) {
    List<TypeVariableName> result = new ArrayList<>();
    for (TypeVariableName type : orig) {
      if (toSort.contains(type)) {
        result.add(type);
      }
    }
    return result;
  }

  static List<List<TypeVariableName>> implTypeParams(List<List<TypeVariableName>> varLifes, List<TypeVariableName> start) {
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


  static List<List<TypeVariableName>> varLifes(List<TypeVariableName> typeParameters,
                                               List<TypeName> steps,
                                               List<TypeVariableName> dependents) {
    List<List<TypeVariableName>> builder = new ArrayList<>(steps.size());
    emptyLists.get().limit(steps.size()).forEach(builder::add);
    List<TypeVariableName> expanded = expand(typeParameters);
    for (TypeVariableName typeParameter : expanded) {
      int start = varLifeStart(typeParameter, steps, dependents);
      if (start >= 0) {
        int end = varLifeEnd(typeParameter, steps);
        for (int i = start; i <= end; i++) {
          builder.get(i).add(typeParameter);
        }
      }
    }
    return builder;
  }

  static List<TypeVariableName> expand(List<TypeVariableName> typeParameters) {
    List<TypeVariableName> builder = new ArrayList<>(typeParameters.size());
    for (TypeVariableName type : typeParameters) {
      List<TypeVariableName> types = extractTypeVars(type);
      for (TypeVariableName t : types) {
        if (!builder.contains(t)) {
          builder.add(t);
        }
      }
    }
    return builder;
  }

  static List<TypeVariableName> dependents(List<TypeVariableName> init,
                                           List<TypeName> parameters) {
    List<TypeVariableName> builder = new ArrayList<>();
    builder.addAll(init);
    for (TypeName type : parameters) {
      for (TypeVariableName t : extractTypeVars(type)) {
        if (referencesAny(t, init)) {
          if (!builder.contains(t)) {
            builder.add(t);
          }
        }
      }
    }
    return builder;
  }

  private static boolean referencesAny(TypeName typeVariableName, List<TypeVariableName> type) {
    for (TypeVariableName variableName : type) {
      if (references(typeVariableName, variableName)) {
        return true;
      }
    }
    return false;
  }

  static int varLifeStart(TypeVariableName typeParameter, List<TypeName> steps, List<TypeVariableName> dependents) {
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

  private VarLife() {
    throw new UnsupportedOperationException("no instances");
  }
}
