package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.GoalOption;
import net.zerobuilder.compiler.generate.DtoModule.Module;
import net.zerobuilder.compiler.generate.DtoRegularGoal.AbstractRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.unmodifiableList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularGoalContextCases;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.transform;
import static net.zerobuilder.compiler.generate.Utilities.upcase;

final class DtoGoalContext {

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

    private final Module module() {
      return goalOption.apply(this).module;
    }

    final AbstractGoalDetails details() {
      return abstractGoalDetails.apply(this);
    }

    final BuildersContext context() {
      return context.apply(this);
    }

    final ClassName implType() {
      String implName = Generator.implName.apply(module(), this);
      return context.apply(this)
          .generatedType.nestedClass(implName);
    }

    final List<ClassName> stepInterfaceTypes() {
      return transform(steps(), step -> contractType().nestedClass(step.thisType));
    }

    final TypeName goalType() {
      return goalType.apply(this);
    }

    final ClassName contractType() {
      String contractName = Generator.contractName.apply(module(), this);
      return context.apply(this)
          .generatedType.nestedClass(contractName);
    }

  }

  interface GoalCases<R> {
    R regularGoal(AbstractRegularGoalContext goal);
    R beanGoal(BeanGoalContext goal);
  }

  static <R> Function<AbstractGoalContext, R> asFunction(final GoalCases<R> cases) {
    return goal -> goal.accept(cases);
  }

  static <R> Function<AbstractGoalContext, R> goalCases(
      Function<? super AbstractRegularGoalContext, ? extends R> regularFunction,
      Function<? super BeanGoalContext, ? extends R> beanFunction) {
    return asFunction(new GoalCases<R>() {
      @Override
      public R regularGoal(AbstractRegularGoalContext goal) {
        return regularFunction.apply(goal);
      }
      @Override
      public R beanGoal(BeanGoalContext goal) {
        return beanFunction.apply(goal);
      }
    });
  }

  static final Function<AbstractRegularGoalContext, BuildersContext> regularContext =
      regularGoalContextCases(
          constructor -> constructor.context,
          method -> method.context);

  private static final Function<AbstractGoalContext, BuildersContext> context =
      goalCases(
          regularContext,
          bean -> bean.context);

  private static final Function<AbstractGoalContext, TypeName> goalType =
      goalCases(
          regular -> regular.regularDetails().goalType,
          bean -> bean.details.goalType);


  private static final Function<AbstractGoalContext, String> goalName =
      goalCases(
          regular -> regular.regularDetails().name,
          bean -> bean.details.name);

  private static final Function<AbstractGoalContext, AbstractGoalDetails> abstractGoalDetails =
      goalCases(
          AbstractRegularGoalContext::regularDetails,
          bGoal -> bGoal.details);

  private static final Function<AbstractGoalContext, List<AbstractStep>> abstractSteps =
      goalCases(
          regular -> unmodifiableList(regular.regularSteps()),
          bean -> unmodifiableList(bean.steps));

  static final Function<AbstractGoalContext, GoalOption> goalOption
      = abstractGoalDetails.andThen(details -> details.option);

  private DtoGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
