package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoalContext.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.GoalCases;
import net.zerobuilder.compiler.generate.DtoGoalContext.GoalContextCommon;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Utilities.statement;
import static net.zerobuilder.compiler.Utilities.upcase;
import static net.zerobuilder.compiler.generate.BuilderContextV.regularInvoke;
import static net.zerobuilder.compiler.generate.DtoGoalContext.always;
import static net.zerobuilder.compiler.generate.DtoGoalContext.getGoalName;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalCases;

final class UpdaterContext {

  static final GoalCases<ClassName> updaterType = always(new Function<GoalContextCommon, ClassName>() {
    @Override
    public ClassName apply(GoalContextCommon goal) {
      return goal.goal.builders.generatedType.nestedClass(
          upcase(getGoalName.apply(goal.goal) + "Updater"));
    }
  });

  private static final GoalCases<ImmutableList<FieldSpec>> fields
      = goalCases(UpdaterContextV.fields, UpdaterContextB.fields);

  private static final GoalCases<ImmutableList<MethodSpec>> updateMethods
      = goalCases(UpdaterContextV.updateMethods, UpdaterContextB.updateMethods);

  private static final GoalCases<MethodSpec> buildMethod =
      always(new Function<GoalContextCommon, MethodSpec>() {
        @Override
        public MethodSpec apply(GoalContextCommon goal) {
          return methodBuilder("build")
              .addModifiers(PUBLIC)
              .returns(goal.goalType)
              .addCode(goal.goal.accept(invoke))
              .addExceptions(goal.thrownTypes)
              .build();
        }
      });

  static TypeSpec defineUpdater(AbstractGoalContext goal) {
    return classBuilder(goal.accept(updaterType))
        .addFields(goal.accept(fields))
        .addMethods(goal.accept(updateMethods))
        .addMethod(goal.accept(buildMethod))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

  private static final Function<BeanGoalContext, CodeBlock> returnBean
      = new Function<BeanGoalContext, CodeBlock>() {
    @Override
    public CodeBlock apply(BeanGoalContext goal) {
      return statement("return this.$N", goal.field);
    }
  };

  private static final GoalCases<CodeBlock> invoke
      = goalCases(regularInvoke, returnBean);

  private UpdaterContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
