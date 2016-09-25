package net.zerobuilder.compiler.generate;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoShared.BeanGoal;
import net.zerobuilder.compiler.analyse.DtoShared.RegularGoal;
import net.zerobuilder.compiler.generate.StepContext.AbstractStep;
import net.zerobuilder.compiler.generate.StepContext.BeansStep;
import net.zerobuilder.compiler.generate.StepContext.RegularStep;

import static com.squareup.javapoet.TypeName.VOID;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.upcase;

public final class GoalContext {

  public static abstract class AbstractContext {
    final BuildersType builders;

    final boolean toBuilder;
    final boolean builder;

    final ClassName contractName;

    @VisibleForTesting
    AbstractContext(BuildersType builders,
                    boolean toBuilder,
                    boolean builder, ClassName contractName) {
      this.builders = builders;
      this.toBuilder = toBuilder;
      this.builder = builder;
      this.contractName = contractName;
    }

    abstract <R> R accept(GoalCases<R> cases);
  }

  static abstract class GoalCases<R> {
    abstract R regularGoal(RegularGoalContext goal);
    abstract R beanGoal(BeanGoalContext goal);
  }

  static abstract class GoalFunction<R> {
    abstract R apply(AbstractContext goal, TypeName goalType, ImmutableList<? extends AbstractStep> parameters);
  }

  static <R> GoalCases<R> always(final GoalFunction<R> function) {
    return new GoalCases<R>() {
      @Override
      R regularGoal(RegularGoalContext goal) {
        return function.apply(goal, goal.goal.goalType, goal.steps);
      }
      @Override
      R beanGoal(BeanGoalContext goal) {
        return function.apply(goal, goal.goal.goalType, goal.steps);
      }
    };
  }

  static <R> Function<AbstractContext, R> goalCasesFunction(final GoalCases<R> cases) {
    return new Function<AbstractContext, R>() {
      @Override
      public R apply(AbstractContext goal) {
        return goal.accept(cases);
      }
    };
  }

  static final GoalCases<ImmutableList<ClassName>> stepInterfaceNames = always(new GoalFunction<ImmutableList<ClassName>>() {
    @Override
    public ImmutableList<ClassName> apply(AbstractContext goal, TypeName goalType, ImmutableList<? extends AbstractStep> parameters) {
      ImmutableList.Builder<ClassName> specs = ImmutableList.builder();
      for (AbstractStep abstractStep : parameters) {
        specs.add(abstractStep.thisType);
      }
      return specs.build();
    }
  });

  static final GoalCases<ClassName> builderImplName = always(new GoalFunction<ClassName>() {
    @Override
    public ClassName apply(AbstractContext goal, TypeName goalType, ImmutableList<? extends AbstractStep> parameters) {
      return goal.builders.generatedType.nestedClass(upcase(goal.accept(getGoalName) + "BuilderImpl"));
    }
  });

  static final GoalCases<String> getGoalName = new GoalCases<String>() {
    @Override
    String regularGoal(RegularGoalContext goal) {
      return goal.goal.name;
    }
    @Override
    String beanGoal(BeanGoalContext goal) {
      return goal.goal.name;
    }
  };

  public final static class RegularGoalContext extends AbstractContext {

    /**
     * original parameter order unless {@link net.zerobuilder.Step} was used
     */
    final ImmutableList<RegularStep> steps;
    final ImmutableList<TypeName> thrownTypes;
    final RegularGoal goal;

    public RegularGoalContext(RegularGoal goal,
                              BuildersType config,
                              boolean toBuilder,
                              boolean builder,
                              ClassName contractName,
                              ImmutableList<RegularStep> steps,
                              ImmutableList<TypeName> thrownTypes) {
      super(config, toBuilder, builder, contractName);
      this.thrownTypes = thrownTypes;
      this.steps = steps;
      this.goal = goal;
    }

    <R> R accept(GoalCases<R> cases) {
      return cases.regularGoal(this);
    }
  }

  public final static class BeanGoalContext extends AbstractContext {

    /**
     * alphabetic order unless {@link net.zerobuilder.Step} was used
     */
    final ImmutableList<BeansStep> steps;
    final BeanGoal goal;

    public BeanGoalContext(BeanGoal goal,
                           BuildersType config,
                           boolean toBuilder,
                           boolean builder,
                           ClassName contractName,
                           ImmutableList<BeansStep> steps) {
      super(config, toBuilder, builder, contractName);
      this.steps = steps;
      this.goal = goal;
    }

    <R> R accept(GoalCases<R> cases) {
      return cases.beanGoal(this);
    }
  }

  static final GoalCases<CodeBlock> invoke
      = new GoalCases<CodeBlock>() {
    @Override
    CodeBlock regularGoal(RegularGoalContext goal) {
      CodeBlock parameters = CodeBlock.of(Joiner.on(", ").join(goal.goal.parameterNames));
      CodeBlock.Builder builder = CodeBlock.builder();
      builder.add(VOID.equals(goal.goal.goalType) ?
          CodeBlock.of("") :
          CodeBlock.of("return "));
      switch (goal.goal.kind) {
        case CONSTRUCTOR:
          return builder
              .addStatement("new $T($L)",
                  goal.goal.goalType, parameters).build();
        case INSTANCE_METHOD:
          return builder.addStatement("$N.$N($L)",
              goal.builders.field, goal.goal.methodName, parameters).build();
        case STATIC_METHOD:
          return builder.addStatement("$T.$N($L)",
              goal.builders.type, goal.goal.methodName, parameters).build();
        default:
          throw new IllegalStateException("unknown kind: " + goal.goal.kind);
      }
    }
    @Override
    CodeBlock beanGoal(BeanGoalContext goal) {
      return CodeBlock.builder()
          .addStatement("return $L", downcase(goal.goal.goalType.simpleName()))
          .build();
    }
  };

  private GoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
