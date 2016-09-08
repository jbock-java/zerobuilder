package net.zerobuilder.compiler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

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

  final BuildConfig config;

  final boolean toBuilder;

  final String goalName;

  final String methodName;

  final UberGoalContext.GoalKind kind;

  final UberGoalContext.Visibility visibility;

  final ImmutableList<TypeName> thrownTypes;

  /**
   * possibly in changed order
   */
  final ImmutableList<ParameterContext> goalParameters;

  final CodeBlock methodParameters;

  @VisibleForTesting
  GoalContext(TypeName goalType,
              ClassName builderType,
              BuildConfig config,
              boolean toBuilder,
              String goalName,
              String methodName,
              UberGoalContext.GoalKind kind,
              UberGoalContext.Visibility visibility,
              ImmutableList<TypeName> thrownTypes,
              ImmutableList<ParameterContext> goalParameters,
              CodeBlock methodParameters) {
    this.goalType = goalType;
    this.builderType = builderType;
    this.config = config;
    this.toBuilder = toBuilder;
    this.goalName = goalName;
    this.methodName = methodName;
    this.kind = kind;
    this.visibility = visibility;
    this.thrownTypes = thrownTypes;
    this.goalParameters = goalParameters;
    this.methodParameters = methodParameters;
  }

  ImmutableList<ClassName> stepInterfaceNames() {
    ImmutableList.Builder<ClassName> specs = ImmutableList.builder();
    for (ParameterContext spec : goalParameters) {
      specs.add(spec.stepContract);
    }
    return specs.build();
  }

  Optional<ClassName> receiverType() {
    return kind == UberGoalContext.GoalKind.INSTANCE_METHOD
        ? Optional.of(config.annotatedType)
        : Optional.<ClassName>absent();
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
    return maybeAddPublic(visibility == UberGoalContext.Visibility.PUBLIC, modifiers);
  }

  static Set<Modifier> maybeAddPublic(boolean add, Modifier... modifiers) {
    ImmutableSet<Modifier> modifierSet = ImmutableSet.copyOf(modifiers);
    if (add && !modifierSet.contains(PUBLIC)) {
      return new ImmutableSet.Builder<Modifier>().addAll(modifierSet).add(PUBLIC).build();
    }
    return modifierSet;
  }

}
