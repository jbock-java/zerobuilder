package net.zerobuilder.modules.generics;

import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static net.zerobuilder.compiler.generate.ZeroUtil.references;

final class VarLife {

  final List<List<TypeVariableName>> varLifes;
  final List<TypeVariableName> typeParameters;
  private final boolean instance;

  private VarLife(List<List<TypeVariableName>> varLifes,
                  List<TypeVariableName> typeParameters, boolean instance) {
    this.varLifes = varLifes;
    this.typeParameters = typeParameters;
    this.instance = instance;
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
    List<TypeVariableName> previous = emptyList();
    for (int i = 0; i < varLifes.size() - 1; i++) {
      List<TypeVariableName> typeNames = varLifes.get(i);
      for (TypeVariableName typeName : typeNames) {
        if (!previous.contains(typeName)) {
          builder.get(i).add(typeName);
        }
      }
      previous = typeNames;
    }
    return chop(builder);
  }

  List<List<TypeVariableName>> typeParams() {
    List<List<TypeVariableName>> builder = new ArrayList<>(varLifes.size() - 1);
    emptyLists.get().limit(varLifes.size() - 1).forEach(builder::add);
    List<TypeVariableName> previous = emptyList();
    List<TypeVariableName> later = new ArrayList<>();
    for (int i = 0; i < varLifes.size() - 1; i++) {
      for (TypeVariableName t : later) {
        if (varLifes.get(i).contains(t)) {
          builder.get(i).add(t);
        }
      }
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
      builder.set(i, sort(builder.get(i)));
      previous = varLifes.get(i);
    }
    return chop(builder);
  }

  List<List<TypeVariableName>> implTypeParams() {
    List<List<TypeVariableName>> builder = new ArrayList<>(varLifes.size() - 1);
    emptyLists.get().limit(varLifes.size() - 1).forEach(builder::add);
    List<TypeVariableName> seen = new ArrayList<>();
    for (int i = 0; i < varLifes.size() - 1; i++) {
      builder.get(i).addAll(seen);
      for (TypeVariableName typeName : varLifes.get(i)) {
        if (!seen.contains(typeName)) {
          seen.add(typeName);
        }
      }
    }
    return chop(builder);
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

  static VarLife create(List<TypeVariableName> typeParameters,
                        List<TypeName> steps,
                        boolean instance) {
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
    return new VarLife(builder, typeParameters, instance);
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
