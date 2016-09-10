package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.UberGoalContext.GoalKind;
import net.zerobuilder.compiler.UberGoalContext.Visibility;

import javax.lang.model.element.Modifier;
import java.util.Set;

import static javax.lang.model.element.Modifier.PUBLIC;

abstract class GoalContext {

  static abstract class Cases<R> {
    abstract R regularGoal(GoalKind kind);
    abstract R fieldGoal(ClassName goalType);
  }

  abstract <R> R accept(Cases<R> cases);

  /**
   * <p>method goal: return type</p>
   * <p>constructor goal: type of enclosing class</p>
   * <p>field goal: type of field</p>
   */
  final TypeName goalType;

  /**
   * enclosing type
   */
  final ClassName builderType;

  final BuilderContext config;

  final boolean toBuilder;

  final String goalName;

  /**
   * <p>method, constructor: parameters, possibly in changed order</p>
   * <p>field goal: setters</p>
   */
  final ImmutableList<ParameterContext> goalParameters;

  final CodeBlock goalCall;

  final ImmutableList<TypeName> thrownTypes;

  abstract Set<Modifier> maybeAddPublic(Modifier... modifiers);

  private GoalContext(TypeName goalType,
                      ClassName builderType,
                      BuilderContext config,
                      boolean toBuilder,
                      String goalName,
                      ImmutableList<ParameterContext> goalParameters,
                      CodeBlock goalCall, ImmutableList<TypeName> thrownTypes) {
    this.goalType = goalType;
    this.builderType = builderType;
    this.config = config;
    this.toBuilder = toBuilder;
    this.goalName = goalName;
    this.goalParameters = goalParameters;
    this.goalCall = goalCall;
    this.thrownTypes = thrownTypes;
  }

  ImmutableList<ClassName> stepInterfaceNames() {
    ImmutableList.Builder<ClassName> specs = ImmutableList.builder();
    for (ParameterContext spec : goalParameters) {
      specs.add(spec.stepContract);
    }
    return specs.build();
  }

  ClassName contractName() {
    return builderType.nestedClass(UberGoalContext.CONTRACT);
  }

  ClassName stepsImplTypeName() {
    return builderType.nestedClass(UberGoalContext.STEPS_IMPL);
  }

  ClassName contractUpdaterName() {
    return contractName()
        .nestedClass(goalName + UberGoalContext.UPDATER_SUFFIX);
  }

  String goalTypeName() {
    return goalTypeName(goalType);
  }

  static String goalTypeName(TypeName goalType) {
    return ((ClassName) goalType.box()).simpleName();
  }

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
    <R> R accept(Cases<R> cases) {
      return cases.regularGoal(kind);
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
    <R> R accept(Cases<R> cases) {
      return cases.fieldGoal(goalType);
    }
  }

}
