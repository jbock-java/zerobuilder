package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import static java.util.Collections.singletonList;

final class GenericsUtil {

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

  private static final class VarWalk implements Iterator<TypeVariableName> {

    private final Stack<TypeVariableName> stack;

    VarWalk(TypeVariableName type) {
      stack = new Stack<>();
      stack.push(type);
    }

    @Override
    public boolean hasNext() {
      return !stack.isEmpty();
    }
    @Override
    public TypeVariableName next() {
      TypeVariableName type = stack.pop();
      if (type instanceof TypeVariableName) {
        for (TypeName bound : type.bounds) {
          if (bound instanceof TypeVariableName) {
            stack.push((TypeVariableName) bound);
          }
        }
      }
      return type;
    }
  }

  static boolean references(TypeName type, TypeName test) {
    if (!(type instanceof ParameterizedTypeName
        || type instanceof TypeVariableName)) {
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

  static List<TypeVariableName> referenced(TypeName type, List<TypeVariableName> options) {
    List<TypeVariableName> builder = new ArrayList<>();
    TypeWalk walk = new TypeWalk(type);
    while (walk.hasNext()) {
      TypeName next = walk.next();
      if (options.contains(next)) {
        if (!builder.contains(next)) {
          builder.add((TypeVariableName) next);
        }
      }
    }
    return builder;
  }

  static List<TypeVariableName> typeVars(TypeVariableName type) {
    if (type.bounds.isEmpty()) {
      return singletonList(type);
    } else {
      VarWalk walk = new VarWalk(type);
      List<TypeVariableName> builder = new ArrayList<>();
      while (walk.hasNext()) {
        builder.add(walk.next());
      }
      return builder;
    }
  }

  private GenericsUtil() {
    throw new UnsupportedOperationException("no instances");
  }
}
