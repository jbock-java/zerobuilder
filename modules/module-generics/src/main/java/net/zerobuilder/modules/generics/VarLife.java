package net.zerobuilder.modules.generics;

import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static net.zerobuilder.modules.generics.GenericsUtil.references;

final class VarLife {

  private static final Supplier<Stream<List<TypeName>>> emptyLists =
      () -> Stream.generate(ArrayList::new);

  static List<List<TypeName>> methodParams(List<List<TypeName>> varLifes) {
    List<List<TypeName>> builder = new ArrayList<>(varLifes.size() - 1);
    emptyLists.get().limit(varLifes.size() - 1).forEach(builder::add);
    List<TypeName> previous = emptyList();
    for (int i = 0; i < varLifes.size() - 1; i++) {
      List<TypeName> typeNames = varLifes.get(i);
      for (TypeName typeName : typeNames) {
        if (!previous.contains(typeName)) {
          builder.get(i).add(typeName);
        }
      }
      previous = typeNames;
    }
    return builder;
  }

  static List<List<TypeName>> typeParams(List<List<TypeName>> varLifes) {
    List<List<TypeName>> builder = new ArrayList<>(varLifes.size() - 1);
    emptyLists.get().limit(varLifes.size() - 1).forEach(builder::add);
    List<TypeName> previous = emptyList();
    List<TypeName> later = new ArrayList<>();
    for (int i = 0; i < varLifes.size() - 1; i++) {
      builder.get(i).addAll(later);
      later.clear();
      for (TypeName t : varLifes.get(i)) {
        if (previous.contains(t)) {
          if (!builder.get(i).contains(t)) {
            builder.get(i).add(t);
          }
        } else {
          later.add(t);
        }
      }
      previous = varLifes.get(i);
    }
    return builder;
  }

  static List<List<TypeName>> implTypeParams(List<List<TypeName>> varLifes) {
    List<List<TypeName>> builder = new ArrayList<>(varLifes.size() - 1);
    emptyLists.get().limit(varLifes.size() - 1).forEach(builder::add);
    List<TypeName> seen = new ArrayList<>();
    for (int i = 0; i < varLifes.size() - 1; i++) {
      builder.get(i).addAll(seen);
      for (TypeName typeName : varLifes.get(i)) {
        if (!seen.contains(typeName)) {
          seen.add(typeName);
        }
      }
    }
    return builder;
  }


  static List<List<TypeName>> varLifes(List<TypeName> typeParameters, List<TypeName> steps) {
    List<List<TypeName>> builder = new ArrayList<>(steps.size());
    emptyLists.get().limit(steps.size()).forEach(builder::add);
    for (TypeName typeParameter : typeParameters) {
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

  private static int varLifeStart(TypeName typeParameter, List<TypeName> steps) {
    for (int i = 0; i < steps.size(); i++) {
      TypeName step = steps.get(i);
      if (references(step, typeParameter)) {
        return i;
      }
    }
    return -1;
  }

  private static int varLifeEnd(TypeName typeParameter, List<TypeName> steps) {
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
