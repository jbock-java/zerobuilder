package net.zerobuilder.compiler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
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

final class GoalContext {

  /**
   * return type (or type of enclosing class, for constructor goals)
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
   * absent iff field goal
   */
  final Optional<GoalKind> kind;

  private final Visibility visibility;

  final ImmutableList<TypeName> thrownTypes;

  /**
   * possibly in changed order
   */
  final ImmutableList<ParameterContext> goalParameters;

  final CodeBlock goalCall;

  @VisibleForTesting
  GoalContext(TypeName goalType,
              ClassName builderType,
              BuilderContext config,
              boolean toBuilder,
              String goalName,
              Optional<GoalKind> kind,
              Visibility visibility,
              ImmutableList<TypeName> thrownTypes,
              ImmutableList<ParameterContext> goalParameters,
              CodeBlock goalCall) {
    this.goalType = goalType;
    this.builderType = builderType;
    this.config = config;
    this.toBuilder = toBuilder;
    this.goalName = goalName;
    this.kind = kind;
    this.visibility = visibility;
    this.thrownTypes = thrownTypes;
    this.goalParameters = goalParameters;
    this.goalCall = goalCall;
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

  Set<Modifier> maybeAddPublic(Modifier... modifiers) {
    return maybeAddPublic(visibility == Visibility.PUBLIC, modifiers);
  }

  static Set<Modifier> maybeAddPublic(boolean add, Modifier... modifiers) {
    ImmutableSet<Modifier> modifierSet = ImmutableSet.copyOf(modifiers);
    if (add && !modifierSet.contains(PUBLIC)) {
      return new ImmutableSet.Builder<Modifier>().addAll(modifierSet).add(PUBLIC).build();
    }
    return modifierSet;
  }

}
