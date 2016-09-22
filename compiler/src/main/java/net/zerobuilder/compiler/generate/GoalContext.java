package net.zerobuilder.compiler.generate;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.GoalContextFactory.GoalKind;
import net.zerobuilder.compiler.generate.ParameterContext.BeansParameterContext;
import net.zerobuilder.compiler.generate.ParameterContext.ExecutableParameterContext;

import static net.zerobuilder.compiler.Utilities.upcase;

public abstract class GoalContext {

  static abstract class GoalCases<R> {
    abstract R executableGoal(GoalContext goal, TypeName goalType, GoalKind kind,
                              ImmutableList<ExecutableParameterContext> parameters,
                              ImmutableList<TypeName> thrownTypes);
    abstract R beanGoal(GoalContext goal, ClassName goalType, ImmutableList<BeansParameterContext> parameters);
  }

  abstract <R> R accept(GoalCases<R> cases);

  static abstract class GoalFunction<R> {
    abstract R apply(GoalContext goal, TypeName goalType, ImmutableList<? extends ParameterContext> parameters);
  }

  static <R> GoalCases<R> always(final GoalFunction<R> function) {
    return new GoalCases<R>() {
      @Override
      R executableGoal(GoalContext goal, TypeName goalType, GoalKind kind,
                       ImmutableList<ExecutableParameterContext> parameters,
                       ImmutableList<TypeName> thrownTypes) {
        return function.apply(goal, goalType, parameters);
      }
      @Override
      R beanGoal(GoalContext goal, ClassName goalType, ImmutableList<BeansParameterContext> parameters) {
        return function.apply(goal, goalType, parameters);
      }
    };
  }

  static <R> Function<GoalContext, R> goalCasesFunction(final GoalCases<R> cases) {
    return new Function<GoalContext, R>() {
      @Override
      public R apply(GoalContext goal) {
        return goal.accept(cases);
      }
    };
  }

  final BuilderType config;

  final boolean toBuilder;
  final boolean builder;

  final ClassName contractName;

  final String goalName;

  /**
   * implementation of {@code updaterImpl.build()} and {@code stepsImpl.build()}
   */
  final CodeBlock goalCall;

  @VisibleForTesting
  GoalContext(BuilderType config,
              boolean toBuilder,
              boolean builder, ClassName contractName, String goalName,
              CodeBlock goalCall) {
    this.config = config;
    this.toBuilder = toBuilder;
    this.builder = builder;
    this.contractName = contractName;
    this.goalName = goalName;
    this.goalCall = goalCall;
  }

  static final GoalCases<ImmutableList<ClassName>> stepInterfaceNames = always(new GoalFunction<ImmutableList<ClassName>>() {
    @Override
    public ImmutableList<ClassName> apply(GoalContext goal, TypeName goalType, ImmutableList<? extends ParameterContext> parameters) {
      ImmutableList.Builder<ClassName> specs = ImmutableList.builder();
      for (ParameterContext parameter : parameters) {
        specs.add(parameter.typeThisStep);
      }
      return specs.build();
    }
  });

  static final GoalCases<ClassName> builderImplName = always(new GoalFunction<ClassName>() {
    @Override
    public ClassName apply(GoalContext goal, TypeName goalType, ImmutableList<? extends ParameterContext> parameters) {
      return goal.config.generatedType.nestedClass(upcase(goal.goalName + "BuilderImpl"));
    }
  });

  public final static class ExecutableGoalContext extends GoalContext {

    final GoalKind kind;

    /**
     * <p>method goal: return type</p>
     * <p>constructor goal: type of enclosing class</p>
     */
    final TypeName goalType;

    /**
     * original parameter order unless Step annotation was used
     */
    final ImmutableList<ExecutableParameterContext> goalParameters;

    final ImmutableList<TypeName> thrownTypes;

    public ExecutableGoalContext(TypeName goalType,
                          BuilderType config,
                          boolean toBuilder,
                          boolean builder,
                          ClassName contractName,
                          GoalKind kind,
                          String goalName,
                          ImmutableList<TypeName> thrownTypes,
                          ImmutableList<ExecutableParameterContext> goalParameters,
                          CodeBlock goalCall) {
      super(config, toBuilder, builder, contractName, goalName, goalCall);
      this.thrownTypes = thrownTypes;
      this.goalParameters = goalParameters;
      this.kind = kind;
      this.goalType = goalType;
    }

    <R> R accept(GoalCases<R> cases) {
      return cases.executableGoal(this, goalType, kind, goalParameters, thrownTypes);
    }
  }

  public final static class BeanGoalContext extends GoalContext {

    /**
     * alphabetic order unless Step annotation was used
     */
    final ImmutableList<BeansParameterContext> goalParameters;
    final ClassName goalType;

    public BeanGoalContext(ClassName goalType,
                    BuilderType config,
                    boolean toBuilder,
                    boolean builder,
                    ClassName contractName,
                    String goalName,
                    ImmutableList<BeansParameterContext> goalParameters,
                    CodeBlock goalCall) {
      super(config, toBuilder, builder, contractName, goalName, goalCall);
      this.goalParameters = goalParameters;
      this.goalType = goalType;
    }

    <R> R accept(GoalCases<R> cases) {
      return cases.beanGoal(this, goalType, goalParameters);
    }
  }

}
