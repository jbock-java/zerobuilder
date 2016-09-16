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
    abstract R regularGoal(GoalContext goal, TypeName goalType, GoalKind kind, ImmutableList<ParameterContext.RegularParameterContext> parameters);
    abstract R fieldGoal(GoalContext goal, ClassName goalType, ImmutableList<ParameterContext.BeansParameterContext> parameters);
  }

  abstract <R> R accept(GoalCases<R> cases);

  static abstract class GoalFunction<R> {
    abstract R apply(GoalContext goal, TypeName goalType, ImmutableList<? extends ParameterContext> parameters);
  }

  static <R> GoalCases<R> always(final GoalFunction<R> function) {
    return new GoalCases<R>() {
      @Override
      R regularGoal(GoalContext goal, TypeName goalType, GoalKind kind, ImmutableList<ParameterContext.RegularParameterContext> parameters) {
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

  /**
   * thrown by {@code updaterImpl.build()} and {@code stepsImpl.build()}
   */
  final ImmutableList<TypeName> thrownTypes;

  abstract Set<Modifier> maybeAddPublic(Modifier... modifiers);

  @VisibleForTesting
  GoalContext(BuilderContext config,
              boolean toBuilder,
              boolean builder, ClassName contractName, String goalName,
              CodeBlock goalCall, ImmutableList<TypeName> thrownTypes) {
    this.config = config;
    this.toBuilder = toBuilder;
    this.builder = builder;
    this.contractName = contractName;
    this.goalName = goalName;
    this.goalCall = goalCall;
    this.thrownTypes = thrownTypes;
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

  static Set<Modifier> maybeAddPublic(boolean add, Modifier... modifiers) {
    ImmutableSet<Modifier> modifierSet = ImmutableSet.copyOf(modifiers);
    if (add && !modifierSet.contains(PUBLIC)) {
      return new ImmutableSet.Builder<Modifier>().addAll(modifierSet).add(PUBLIC).build();
    }
    return modifierSet;
  }

  final static class RegularGoalContext extends GoalContext {

    final GoalKind kind;
    final Visibility visibility;

    /**
     * <p>method goal: return type</p>
     * <p>constructor goal: type of enclosing class</p>
     */
    final TypeName goalType;

    /**
     * original parameter order unless Step annotation was used
     */
    final ImmutableList<ParameterContext.RegularParameterContext> goalParameters;

    RegularGoalContext(TypeName goalType,
                       BuilderContext config,
                       boolean toBuilder,
                       boolean builder,
                       ClassName contractName,
                       GoalKind kind,
                       String goalName,
                       Visibility visibility,
                       ImmutableList<TypeName> thrownTypes,
                       ImmutableList<ParameterContext.RegularParameterContext> goalParameters,
                       CodeBlock goalCall) {
      super(config, toBuilder, builder, contractName, goalName, goalCall, thrownTypes);
      this.goalParameters = goalParameters;
      this.visibility = visibility;
      this.kind = kind;
      this.goalType = goalType;
    }

    Set<Modifier> maybeAddPublic(Modifier... modifiers) {
      return maybeAddPublic(visibility == Visibility.PUBLIC, modifiers);
    }
    <R> R accept(GoalCases<R> cases) {
      return cases.regularGoal(this, goalType, kind, goalParameters);
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
      super(config, toBuilder, builder, contractName, goalName, goalCall, ImmutableList.<TypeName>of());
      this.goalParameters = goalParameters;
      this.goalType = goalType;
    }

    Set<Modifier> maybeAddPublic(Modifier... modifiers) {
      return maybeAddPublic(true, modifiers);
    }
    <R> R accept(GoalCases<R> cases) {
      return cases.fieldGoal(this, goalType, goalParameters);
    }
  }

}
