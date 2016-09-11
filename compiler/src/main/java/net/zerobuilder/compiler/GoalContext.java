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

abstract class GoalContext {

  static abstract class GoalCases<R> {
    abstract R regularGoal(GoalContext goal, GoalKind kind);
    abstract R fieldGoal(GoalContext goal, ClassName goalType);
  }

  abstract <R> R accept(GoalCases<R> cases);

  static <R> GoalCases<R> always(final Function<GoalContext, R> function) {
    return new GoalCases<R>() {
      @Override
      R regularGoal(GoalContext goal, GoalKind kind) {
        return function.apply(goal);
      }
      @Override
      R fieldGoal(GoalContext goal, ClassName goalType) {
        return function.apply(goal);
      }
    };
  }

  /**
   * <p>method goal: return type</p>
   * <p>constructor goal: type of enclosing class</p>
   * <p>field goal: type of field</p>
   */
  final TypeName goalType;

  final ClassName generatedType;

  final BuilderContext config;

  final boolean toBuilder;

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
  GoalContext(TypeName goalType,
                      ClassName generatedType,
                      BuilderContext config,
                      boolean toBuilder,
                      String goalName,
                      ImmutableList<ParameterContext> goalParameters,
                      CodeBlock goalCall, ImmutableList<TypeName> thrownTypes) {
    this.goalType = goalType;
    this.generatedType = generatedType;
    this.config = config;
    this.toBuilder = toBuilder;
    this.goalName = goalName;
    this.goalParameters = goalParameters;
    this.goalCall = goalCall;
    this.thrownTypes = thrownTypes;
  }

  static final GoalCases<ImmutableList<ClassName>> stepInterfaceNames = always(new Function<GoalContext, ImmutableList<ClassName>>() {
    @Override
    public ImmutableList<ClassName> apply(GoalContext goal) {
      ImmutableList.Builder<ClassName> specs = ImmutableList.builder();
      for (ParameterContext spec : goal.goalParameters) {
        specs.add(spec.stepContract);
      }
      return specs.build();
    }
  });

  static final GoalCases<ClassName> contractName = always(new Function<GoalContext, ClassName>() {
    @Override
    public ClassName apply(GoalContext goal) {
      return goal.generatedType.nestedClass(GoalContextFactory.CONTRACT);
    }
  });

  static final GoalCases<ClassName> stepsImplTypeName = always(new Function<GoalContext, ClassName>() {
    @Override
    public ClassName apply(GoalContext goal) {
      return goal.generatedType.nestedClass(GoalContextFactory.STEPS_IMPL);
    }
  });

  static final GoalCases<ClassName> contractUpdaterName = always(new Function<GoalContext, ClassName>() {
    @Override
    public ClassName apply(GoalContext goal) {
      return goal.accept(contractName)
          .nestedClass(goal.goalName + GoalContextFactory.UPDATER_SUFFIX);
    }
  });

  static final GoalCases<String> goalTypeName = always(new Function<GoalContext, String>() {
    @Override
    public String apply(GoalContext goal) {
      return ((ClassName) goal.goalType.box()).simpleName();
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

    RegularGoalContext(TypeName goalType,
                       ClassName builderType,
                       BuilderContext config,
                       boolean toBuilder,
                       GoalKind kind,
                       String goalName,
                       Visibility visibility,
                       ImmutableList<TypeName> thrownTypes,
                       ImmutableList<ParameterContext> goalParameters,
                       CodeBlock goalCall) {
      super(goalType, builderType, config, toBuilder, goalName, goalParameters, goalCall, thrownTypes);
      this.visibility = visibility;
      this.kind = kind;
    }

    Set<Modifier> maybeAddPublic(Modifier... modifiers) {
      return maybeAddPublic(visibility == Visibility.PUBLIC, modifiers);
    }
    <R> R accept(GoalCases<R> cases) {
      return cases.regularGoal(this, kind);
    }
  }

  final static class FieldGoalContext extends GoalContext {

    final ClassName goalType;

    FieldGoalContext(ClassName goalType,
                     ClassName builderType,
                     BuilderContext config,
                     boolean toBuilder,
                     String goalName,
                     ImmutableList<ParameterContext> goalParameters,
                     CodeBlock goalCall) {
      super(goalType, builderType, config, toBuilder, goalName, goalParameters, goalCall, ImmutableList.<TypeName>of());
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
