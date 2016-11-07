package net.zerobuilder.modules.generics;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoRegularStep;
import net.zerobuilder.compiler.generate.DtoRegularStep.SimpleRegularStep;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static net.zerobuilder.modules.generics.GenericsUtil.references;

final class VarLife {

  static List<List<TypeName>> varLifes(List<TypeName> typeParameters, List<SimpleRegularStep> steps) {
    Stream<List<TypeName>> generate = Stream.generate(ArrayList::new);
    List<List<TypeName>> builder = new ArrayList<>(steps.size());
    generate.limit(steps.size()).forEach(builder::add);
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

  private static int varLifeStart(TypeName typeParameter, List<SimpleRegularStep> steps) {
    for (int i = 0; i < steps.size(); i++) {
      SimpleRegularStep step = steps.get(i);
      if (references(step.parameter.type, typeParameter)) {
        return i;
      }
    }
    return -1;
  }

  private static int varLifeEnd(TypeName typeParameter, List<SimpleRegularStep> steps) {
    for (int i = steps.size() - 1; i >= 0; i--) {
      SimpleRegularStep step = steps.get(i);
      if (references(step.parameter.type, typeParameter)) {
        return i;
      }
    }
    throw new IllegalStateException(typeParameter + " not found");
  }

  private VarLife() {
    throw new UnsupportedOperationException("no instances");
  }
}
