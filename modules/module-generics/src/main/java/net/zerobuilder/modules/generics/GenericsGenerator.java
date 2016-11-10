package net.zerobuilder.modules.generics;

import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;

import java.util.List;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.modules.generics.GenericsContract.contractType;
import static net.zerobuilder.modules.generics.GenericsContract.stepInterfaces;

final class GenericsGenerator {

  private final List<TypeSpec> stepSpecs;
  private final SimpleStaticMethodGoalContext goal;

  private GenericsGenerator(List<TypeSpec> stepSpecs, SimpleStaticMethodGoalContext goal) {
    this.stepSpecs = stepSpecs;
    this.goal = goal;
  }

  TypeSpec defineContract() {
    return classBuilder(contractType(goal))
        .addTypes(stepSpecs)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE)
            .build())
        .build();
  }

  static GenericsGenerator create(SimpleStaticMethodGoalContext goal) {
    return new GenericsGenerator(stepInterfaces(goal), goal);
  }
}
