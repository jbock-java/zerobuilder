package net.zerobuilder.compiler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.GoalContextFactory.GoalKind;
import net.zerobuilder.compiler.GoalContextFactory.Visibility;

import javax.lang.model.element.Modifier;
import java.util.Set;

import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.upcase;

abstract class GoalContext {

  static abstract class GoalCases<R> {
    abstract R regularGoal(GoalContext goal, TypeName goalType, GoalKind kind,
                           ImmutableList<ParameterContext.RegularParameterContext> parameters,
                           ImmutableList<TypeName> thrownTypes);
    abstract R fieldGoal(GoalContext goal, ClassName goalType, ImmutableList<ParameterContext.BeansParameterContext> parameters);
  }

  abstract <R> R accept(GoalCases<R> cases);

  static abstract class GoalFunction<R> {
    abstract R apply(GoalContext goal, TypeName goalType, ImmutableList<? extends ParameterContext> parameters);
  }

  static <R> GoalCases<R> always(final GoalFunction<R> function) {
    return new GoalCases<R>() {
      @Override
      R regularGoal(GoalContext goal, TypeName goalType, GoalKind kind,
                    ImmutableList<ParameterContext.RegularParameterContext> parameters,
                    ImmutableList<TypeName> thrownTypes) {
        return function.apply(goal, goalType, parameters);
      }
      @Override
      R fieldGoal(GoalContext goal, ClassName goalType, ImmutableList<ParameterContext.BeansParameterContext> parameters) {
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

  final BuilderContext config;

  final boolean toBuilder;
  final boolean builder;

  final ClassName contractName;

  /**
   * Always starts with a lower case character.
   */
  final String goalName;

  /**
   * contents of {@code updaterImpl.build()} and {@code stepsImpl.build()}
   */
  final CodeBlock goalCall;

  @VisibleForTesting
  GoalContext(BuilderContext config,
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

  final static class RegularGoalContext extends GoalContext {

    final GoalKind kind;

    /**
     * <p>method goal: return type</p>
     * <p>constructor goal: type of enclosing class</p>
     */
    final TypeName goalType;

    /**
     * original parameter order unless Step annotation was used
     */
    final ImmutableList<ParameterContext.RegularParameterContext> goalParameters;

    final ImmutableList<TypeName> thrownTypes;

    RegularGoalContext(TypeName goalType,
                       BuilderContext config,
                       boolean toBuilder,
                       boolean builder,
                       ClassName contractName,
                       GoalKind kind,
                       String goalName,
                       ImmutableList<TypeName> thrownTypes,
                       ImmutableList<ParameterContext.RegularParameterContext> goalParameters,
                       CodeBlock goalCall) {
      super(config, toBuilder, builder, contractName, goalName, goalCall);
      this.thrownTypes = thrownTypes;
      this.goalParameters = goalParameters;
      this.kind = kind;
      this.goalType = goalType;
    }

    <R> R accept(GoalCases<R> cases) {
      return cases.regularGoal(this, goalType, kind, goalParameters, thrownTypes);
    }
  }

  final static class FieldGoalContext extends GoalContext {

    /**
     * alphabetic order unless Step annotation was used
     */
    final ImmutableList<ParameterContext.BeansParameterContext> goalParameters;
    final ClassName goalType;

    FieldGoalContext(ClassName goalType,
                     BuilderContext config,
                     boolean toBuilder,
                     boolean builder,
                     ClassName contractName,
                     String goalName,
                     ImmutableList<ParameterContext.BeansParameterContext> goalParameters,
                     CodeBlock goalCall) {
      super(config, toBuilder, builder, contractName, goalName, goalCall);
      this.goalParameters = goalParameters;
      this.goalType = goalType;
    }

    <R> R accept(GoalCases<R> cases) {
      return cases.fieldGoal(this, goalType, goalParameters);
    }
  }

}
