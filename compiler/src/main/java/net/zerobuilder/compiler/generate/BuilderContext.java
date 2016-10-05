package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.GoalContextCommon;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoGoalContext.always;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builderImplType;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalCases;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalName;
import static net.zerobuilder.compiler.generate.DtoGoalContext.stepInterfaceTypes;
import static net.zerobuilder.compiler.generate.StepContext.asStepInterface;

final class BuilderContext {

  private static final Function<AbstractGoalContext, ImmutableList<FieldSpec>> fields
      = goalCases(BuilderContextV.fields, BuilderContextB.fields);

  private static final Function<AbstractGoalContext, ImmutableList<TypeSpec>> stepInterfaces
      = always(new Function<GoalContextCommon, ImmutableList<TypeSpec>>() {
    @Override
    public ImmutableList<TypeSpec> apply(GoalContextCommon goal) {
      return FluentIterable.from(goal.parameters).transform(asStepInterface).toList();
    }
  });

  private static final Function<AbstractGoalContext, ImmutableList<MethodSpec>> steps
      = goalCases(BuilderContextV.steps, BuilderContextB.steps);

  static TypeSpec defineBuilderImpl(AbstractGoalContext goal) {
    return classBuilder(builderImplType.apply(goal))
        .addSuperinterfaces(stepInterfaceTypes.apply(goal))
        .addFields(fields.apply(goal))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .addMethods(steps.apply(goal))
        .addModifiers(STATIC, FINAL)
        .build();
  }

  static TypeSpec defineContract(AbstractGoalContext goal) {
    return classBuilder(contractName(goal))
        .addTypes(stepInterfaces.apply(goal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

  private static ClassName contractName(AbstractGoalContext goal) {
    return DtoGoalContext.contractName(goalName.apply(goal), goal.builders);
  }

  private BuilderContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
