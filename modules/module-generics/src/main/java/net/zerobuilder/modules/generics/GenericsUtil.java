package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

final class GenericsUtil {

  static final class TypeWalk implements Iterator<TypeName> {

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
      return type;
    }
  }

  static boolean references(TypeName type, TypeName test) {
    if (!(type instanceof ParameterizedTypeName)) {
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

  static List<TypeVariableName> typeVars(TypeName type) {
    List<TypeVariableName> builder = new ArrayList<>();
    TypeWalk walk = new TypeWalk(type);
    while (walk.hasNext()) {
      TypeName typeName = walk.next();
      if (typeName instanceof TypeVariableName) {
        if (!builder.contains(typeName)) {
          builder.add((TypeVariableName) typeName);
        }
      }
    }
    return builder;
  }

  private GenericsUtil() {
    throw new UnsupportedOperationException("no instances");
  }
}
