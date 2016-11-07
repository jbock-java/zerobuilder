package net.zerobuilder.modules.generics;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoRegularStep;

import java.util.ArrayList;
import java.util.List;

import static net.zerobuilder.modules.generics.GenericsUtil.references;

final class VarLife {

  final TypeName typeParameter;
  final int start;
  final int end;

  private VarLife(TypeName typeParameter, int start, int end) {
    this.typeParameter = typeParameter;
    if (start < 0) {
      throw new IllegalArgumentException("start: " + start);
    }
    if (start > end) {
      throw new IllegalArgumentException(start + " > " + end);
    }
    this.start = start;
    this.end = end;
  }

  static List<VarLife> varLifes(List<TypeName> typeParameters, List<DtoRegularStep.SimpleRegularStep> steps) {
    List<VarLife> builder = new ArrayList<>(steps.size());
    for (TypeName typeParameter : typeParameters) {
      int start = varLifeStart(typeParameter, steps);
      if (start >= 0) {
        int end = varLifeEnd(typeParameter, steps);
        builder.add(new VarLife(typeParameter, start, end));
      }
    }
    return builder;
  }

  private static int varLifeStart(TypeName typeParameter, List<DtoRegularStep.SimpleRegularStep> steps) {
    for (int i = 0; i < steps.size(); i++) {
      DtoRegularStep.SimpleRegularStep step = steps.get(i);
      if (references(step.parameter.type, typeParameter)) {
        return i;
      }
    }
    return -1;
  }

  private static int varLifeEnd(TypeName typeParameter, List<DtoRegularStep.SimpleRegularStep> steps) {
    for (int i = steps.size() - 1; i >= 0; i--) {
      DtoRegularStep.SimpleRegularStep step = steps.get(i);
      if (references(step.parameter.type, typeParameter)) {
        return i;
      }
    }
    throw new IllegalStateException(typeParameter + " not found");
  }

  @Override
  public String toString() {
    return '(' + String.join(",", typeParameter.toString(), Integer.toString(start), Integer.toString(end)) + ')';
  }
}
