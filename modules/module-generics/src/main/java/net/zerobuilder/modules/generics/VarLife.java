package net.zerobuilder.modules.generics;

import com.squareup.javapoet.TypeName;

import java.security.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static net.zerobuilder.modules.generics.GenericsUtil.references;

final class VarLife {

  private static final Supplier<Stream<List<TypeName>>> emptyLists =
      () -> Stream.generate(ArrayList::new);

  static List<List<TypeName>> methodParams(List<List<TypeName>> varLifes) {
    List<List<TypeName>> builder = new ArrayList<>(varLifes.size());
    emptyLists.get().limit(varLifes.size()).forEach(builder::add);
    List<TypeName> previous = emptyList();
    for (int i = 0; i < varLifes.size(); i++) {
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
    List<List<TypeName>> builder = new ArrayList<>(varLifes.size());
    emptyLists.get().limit(varLifes.size()).forEach(builder::add);
    List<TypeName> previous = emptyList();
    List<TypeName> previouslyAdded = emptyList();
    for (int i = 0; i < varLifes.size(); i++) {
      List<TypeName> typeNames = varLifes.get(i);
      for (TypeName typeName : typeNames) {
        if (previous.contains(typeName)) {
          builder.get(i).add(typeName);
        }
      }
      for (TypeName typeName : previouslyAdded) {
        if (!builder.get(i).contains(typeName)) {
          builder.get(i).add(typeName);
        }
      }
      previouslyAdded = new ArrayList<>();
      for (TypeName typeName : typeNames) {
        if (!previous.contains(typeName)) {
          previouslyAdded.add(typeName);
        }
      }
      previous = typeNames;
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
