package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoal.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static java.util.Collections.unmodifiableList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularSteps;
import static net.zerobuilder.compiler.generate.Updater.updaterType;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.Utilities.statement;
import static net.zerobuilder.compiler.generate.Utilities.transform;
import static net.zerobuilder.compiler.generate.Utilities.upcase;

final class DtoGoalContext {

  interface IGoal {

    AbstractGoalContext withContext(BuildersContext context);
  }

  static abstract class AbstractGoalContext {

    abstract <R> R accept(GoalCases<R> cases);

    private final Supplier<FieldSpec> updaterField =
        () -> updaterField(this);

    private final Supplier<FieldSpec> builderField =
        () -> builderField(this);

    final FieldSpec updaterField() {
      return updaterField.get();
    }

    final FieldSpec builderField() {
      return builderField.get();
    }

    final String name() {
      return goalName.apply(this);
    }

    final DtoGoal.GoalOptions goalOptions() {
      return goalOptions.apply(this);
    }

    private static final Function<AbstractGoalContext, DtoGoal.GoalOptions> goalOptions
        = abstractGoalDetails.andThen(details -> details.goalOptions);

    private static FieldSpec updaterField(AbstractGoalContext context) {
      ClassName type = updaterType(context);
      return FieldSpec.builder(type, downcase(goalName.apply(context) + "Updater"), PRIVATE, FINAL)
          .initializer("new $T()", type)
          .build();
    }

    private static FieldSpec builderField(AbstractGoalContext context) {
      ClassName type = builderImplType(context);
      return FieldSpec.builder(type, downcase(goalName.apply(context) + "BuilderImpl"), PRIVATE, FINAL)
          .initializer("new $T()", type)
          .build();
    }
  }

  interface GoalCases<R> {
    R regularGoal(RegularGoalContext goal);
    R beanGoal(BeanGoalContext goal);
  }

  static <R> Function<AbstractGoalContext, R> asFunction(final GoalCases<R> cases) {
    return goal -> goal.accept(cases);
  }

  static <R> Function<AbstractGoalContext, R>
  goalCases(final Function<? super RegularGoalContext, ? extends R> regularFunction,
            final Function<? super BeanGoalContext, ? extends R> beanFunction) {
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
        step -> Builder.contractName(goal).nestedClass(step.thisType));
  }

  static final Function<AbstractGoalContext, BuildersContext> buildersContext
      = asFunction(new GoalCases<BuildersContext>() {
    @Override
    public BuildersContext regularGoal(RegularGoalContext goal) {
      return DtoRegularGoal.buildersContext.apply(goal);
    }
    @Override
    public BuildersContext beanGoal(BeanGoalContext goal) {
      return goal.context;
    }
  });

  static ClassName builderImplType(AbstractGoalContext goal) {
    return buildersContext.apply(goal).generatedType.nestedClass(
        upcase(goalName.apply(goal) + "BuilderImpl"));
  }

  static final Function<AbstractGoalContext, TypeName> goalType =
      asFunction(new GoalCases<TypeName>() {
        @Override
        public TypeName regularGoal(RegularGoalContext goal) {
          RegularGoalDetails regularGoalDetails = DtoRegularGoal.goalDetails.apply(goal);
          return regularGoalDetails.goalType;
        }
        @Override
        public TypeName beanGoal(BeanGoalContext goal) {
          return goal.goal.details.goalType;
        }
      });


  private static final Function<AbstractGoalContext, String> goalName = asFunction(new GoalCases<String>() {
    @Override
    public String regularGoal(RegularGoalContext goal) {
      RegularGoalDetails regularGoalDetails = DtoRegularGoal.goalDetails.apply(goal);
      return regularGoalDetails.name;
    }
    @Override
    public String beanGoal(BeanGoalContext goal) {
      return goal.goal.details.name;
    }
  });

  static final Function<AbstractGoalContext, AbstractGoalDetails> abstractGoalDetails =
      goalCases(
          DtoRegularGoal.goalDetails,
          bGoal -> bGoal.goal.details);

  static final Function<AbstractGoalContext, List<AbstractStep>> abstractSteps =
      goalCases(
          rGoal -> unmodifiableList(regularSteps.apply(rGoal)),
          bGoal -> unmodifiableList(bGoal.steps()));

  static final Function<AbstractGoalContext, MethodSpec> builderConstructor =
      goalCases(
          DtoRegularGoal.builderConstructor,
          bGoal -> constructorBuilder()
              .addModifiers(PRIVATE)
              .addExceptions(bGoal.context.lifecycle == REUSE_INSTANCES
                  ? Collections.emptyList()
                  : bGoal.goal.thrownTypes)
              .addCode(bGoal.context.lifecycle == REUSE_INSTANCES
                  ? emptyCodeBlock
                  : statement("this.$N = new $T()", bGoal.bean(), bGoal.type()))
              .build());

  static ClassName contractName(String goalName, ClassName generatedType) {
    return generatedType.nestedClass(upcase(goalName + "Builder"));
  }

  private DtoGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
