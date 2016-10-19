package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.GoalOption;
import net.zerobuilder.compiler.generate.DtoModule.Module;
import net.zerobuilder.compiler.generate.DtoRegularGoal.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.unmodifiableList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularSteps;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.transform;
import static net.zerobuilder.compiler.generate.Utilities.upcase;

final class DtoGoalContext {

  interface IGoal {

    AbstractGoalContext withContext(BuildersContext context);
  }

  static abstract class AbstractGoalContext {

    abstract <R> R accept(GoalCases<R> cases);

    final FieldSpec cacheField() {
      ClassName type = implType();
      return FieldSpec.builder(type, downcase(type.simpleName()), PRIVATE, FINAL)
          .initializer("new $T()", type)
          .build();
    }

    final List<AbstractStep> steps() {
      return abstractSteps.apply(this);
    }

    final String name() {
      return goalName.apply(this);
    }

    final String methodName() {
      return name() + upcase(module().name());
    }

    final Module module() {
      return goalOption.apply(this).module;
    }

    final AbstractGoalDetails details() {
      return abstractGoalDetails.apply(this);
    }

    final ClassName implType() {
      String implName = Generator.implName.apply(module(), this);
      return buildersContext.apply(this)
          .generatedType.nestedClass(implName);
    }

    final ClassName contractType() {
      String contractName = Generator.contractName.apply(module(), this);
      return buildersContext.apply(this)
          .generatedType.nestedClass(contractName);
    }

  }

  interface GoalCases<R> {
    R regularGoal(RegularGoalContext goal);
    R beanGoal(BeanGoalContext goal);
  }

  static <R> Function<AbstractGoalContext, R> asFunction(final GoalCases<R> cases) {
    return goal -> goal.accept(cases);
  }

  static <R> Function<AbstractGoalContext, R> goalCases(
      Function<? super RegularGoalContext, ? extends R> regularFunction,
      Function<? super BeanGoalContext, ? extends R> beanFunction) {
    return asFunction(new GoalCases<R>() {
      @Override
      public R regularGoal(RegularGoalContext goal) {
        return regularFunction.apply(goal);
      }
      @Override
      public R beanGoal(BeanGoalContext goal) {
        return beanFunction.apply(goal);
      }
    });
  }

  static List<ClassName> stepInterfaceTypes(AbstractGoalContext goal) {
    return transform(
        abstractSteps.apply(goal),
        step -> goal.contractType().nestedClass(step.thisType));
  }

  private static final Function<AbstractGoalContext, BuildersContext> buildersContext =
      goalCases(
          DtoRegularGoal.buildersContext,
          bean -> bean.context);

  static final Function<AbstractGoalContext, TypeName> goalType =
      goalCases(
          regular -> DtoRegularGoal.goalDetails.apply(regular).goalType,
          bean -> bean.goal.details.goalType);


  private static final Function<AbstractGoalContext, String> goalName =
      goalCases(
          regular -> DtoRegularGoal.goalDetails.apply(regular).name,
          bean -> bean.goal.details.name);

  private static final Function<AbstractGoalContext, AbstractGoalDetails> abstractGoalDetails =
      goalCases(
          DtoRegularGoal.goalDetails,
          bGoal -> bGoal.goal.details);

  private static final Function<AbstractGoalContext, List<AbstractStep>> abstractSteps =
      goalCases(
          regular -> unmodifiableList(regularSteps.apply(regular)),
          bean -> unmodifiableList(bean.goal.steps));

  private static final Function<AbstractGoalContext, GoalOption> goalOption
      = abstractGoalDetails.andThen(details -> details.goalOption);

  private DtoGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
