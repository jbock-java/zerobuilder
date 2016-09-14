package net.zerobuilder.compiler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.GoalContextFactory.GoalKind;
import net.zerobuilder.compiler.GoalContextFactory.Visibility;

import javax.lang.model.element.Modifier;
import java.util.Set;

import static javax.lang.model.element.Modifier.PUBLIC;

abstract class GoalContext {

  static abstract class GoalCases<R> {
    abstract R regularGoal(GoalContext goal, TypeName goalType, GoalKind kind);
    abstract R fieldGoal(GoalContext goal, ClassName goalType);
  }

  abstract <R> R accept(GoalCases<R> cases);

  static abstract class GoalFunction<R> {
    abstract R apply(GoalContext goal, TypeName goalType);
  }

  static <R> GoalCases<R> always(final GoalFunction<R> function) {
    return new GoalCases<R>() {
      @Override
      R regularGoal(GoalContext goal, TypeName goalType, GoalKind kind) {
        return function.apply(goal, goalType);
      }
      @Override
      R fieldGoal(GoalContext goal, ClassName goalType) {
        return function.apply(goal, goalType);
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

  /**
   * Always starts with a lower case character.
   */
  final String goalName;

  /**
   * <p>method, constructor: parameters, possibly in changed order</p>
   * <p>field goal: setters</p>
   */
  final ImmutableList<ParameterContext> goalParameters;

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
              String goalName,
              ImmutableList<ParameterContext> goalParameters,
              CodeBlock goalCall, ImmutableList<TypeName> thrownTypes) {
    this.config = config;
    this.toBuilder = toBuilder;
    this.goalName = goalName;
    this.goalParameters = goalParameters;
    this.goalCall = goalCall;
    this.thrownTypes = thrownTypes;
  }

  static final GoalCases<ImmutableList<ClassName>> stepInterfaceNames = always(new GoalFunction<ImmutableList<ClassName>>() {
    @Override
    public ImmutableList<ClassName> apply(GoalContext goal, TypeName goalType) {
      ImmutableList.Builder<ClassName> specs = ImmutableList.builder();
      for (ParameterContext spec : goal.goalParameters) {
        specs.add(spec.stepContract);
      }
      return specs.build();
    }
  });

  static final GoalCases<ClassName> contractName = always(new GoalFunction<ClassName>() {
    @Override
    public ClassName apply(GoalContext goal, TypeName goalType) {
      return goal.config.generatedType.nestedClass(goal.goalName + "Builder");
    }
  });

  static final GoalCases<ClassName> stepsImplTypeName = always(new GoalFunction<ClassName>() {
    @Override
    public ClassName apply(GoalContext goal, TypeName goalType) {
      return goal.config.generatedType.nestedClass(goal.goalName + "BuilderImpl");
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

    RegularGoalContext(TypeName goalType,
                       BuilderContext config,
                       boolean toBuilder,
                       GoalKind kind,
                       String goalName,
                       Visibility visibility,
                       ImmutableList<TypeName> thrownTypes,
                       ImmutableList<ParameterContext> goalParameters,
                       CodeBlock goalCall) {
      super(config, toBuilder, goalName, goalParameters, goalCall, thrownTypes);
      this.visibility = visibility;
      this.kind = kind;
      this.goalType = goalType;
    }

    Set<Modifier> maybeAddPublic(Modifier... modifiers) {
      return maybeAddPublic(visibility == Visibility.PUBLIC, modifiers);
    }
    <R> R accept(GoalCases<R> cases) {
      return cases.regularGoal(this, goalType, kind);
    }
  }

  final static class FieldGoalContext extends GoalContext {

    final ClassName goalType;

    FieldGoalContext(ClassName goalType,
                     BuilderContext config,
                     boolean toBuilder,
                     String goalName,
                     ImmutableList<ParameterContext> goalParameters,
                     CodeBlock goalCall) {
      super(config, toBuilder, goalName, goalParameters, goalCall, ImmutableList.<TypeName>of());
      this.goalType = goalType;
    }

    Set<Modifier> maybeAddPublic(Modifier... modifiers) {
      return maybeAddPublic(true, modifiers);
    }
    <R> R accept(GoalCases<R> cases) {
      return cases.fieldGoal(this, goalType);
    }
  }

}
