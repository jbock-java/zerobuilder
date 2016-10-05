package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoGoalContext.abstractSteps;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builderImplType;
import static net.zerobuilder.compiler.generate.DtoGoalContext.buildersContext;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalCases;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalName;
import static net.zerobuilder.compiler.generate.DtoGoalContext.stepInterfaceTypes;
import static net.zerobuilder.compiler.generate.StepContext.asStepInterface;

final class BuilderContext {

  private static final Function<AbstractGoalContext, ImmutableList<FieldSpec>> fields
      = goalCases(BuilderContextV.fields, BuilderContextB.fields);

  private static ImmutableList<TypeSpec> stepInterfaces(AbstractGoalContext goal) {
    return FluentIterable.from(abstractSteps.apply(goal))
        .transform(asStepInterface)
        .toList();

  }

  private static final Function<AbstractGoalContext, ImmutableList<MethodSpec>> steps
      = goalCases(BuilderContextV.steps, BuilderContextB.steps);

  static TypeSpec defineBuilderImpl(AbstractGoalContext goal) {
    return classBuilder(builderImplType(goal))
        .addSuperinterfaces(stepInterfaceTypes(goal))
        .addFields(fields.apply(goal))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .addMethods(steps.apply(goal))
        .addModifiers(STATIC, FINAL)
        .build();
  }

  static TypeSpec defineContract(AbstractGoalContext goal) {
    return classBuilder(contractName(goal))
        .addTypes(stepInterfaces(goal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

  private static ClassName contractName(AbstractGoalContext goal) {
    DtoBuilders.BuildersContext buildersContext = DtoGoalContext.buildersContext.apply(goal);
    return DtoGoalContext.contractName(goalName.apply(goal), buildersContext.generatedType);
  }

  private BuilderContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
