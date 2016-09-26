package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.GoalCases;
import net.zerobuilder.compiler.generate.DtoGoalContext.GoalContextCommon;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoGoalContext.always;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builderImplName;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalCases;
import static net.zerobuilder.compiler.generate.DtoGoalContext.stepInterfaceNames;
import static net.zerobuilder.compiler.generate.StepContext.asStepInterface;

final class BuilderContext {

  private static final GoalCases<ImmutableList<FieldSpec>> fields
      = goalCases(BuilderContextV.fields, BuilderContextB.fields);

  private static final GoalCases<ImmutableList<TypeSpec>> stepInterfaces
      = always(new Function<GoalContextCommon, ImmutableList<TypeSpec>>() {
    @Override
    public ImmutableList<TypeSpec> apply(GoalContextCommon goal) {
      return FluentIterable.from(goal.parameters).transform(asStepInterface).toList();
    }
  });

  private static final GoalCases<ImmutableList<MethodSpec>> stepsButLast
      = goalCases(BuilderContextV.stepsButLast, BuilderContextB.stepsButLast);

  private static final GoalCases<ImmutableList<MethodSpec>> lastStep
      = goalCases(BuilderContextV.lastStep, BuilderContextB.lastStep);

  static TypeSpec defineBuilderImpl(AbstractGoalContext goal) {
    return classBuilder(goal.accept(builderImplName))
        .addSuperinterfaces(goal.accept(stepInterfaceNames))
        .addFields(goal.accept(fields))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .addMethods(goal.accept(stepsButLast))
        .addMethods(goal.accept(lastStep))
        .addModifiers(FINAL, STATIC)
        .build();
  }

  static TypeSpec defineContract(AbstractGoalContext goal) {
    return classBuilder(goal.contractName)
        .addTypes(goal.accept(stepInterfaces))
        .addModifiers(PUBLIC, FINAL, STATIC)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

  private BuilderContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
