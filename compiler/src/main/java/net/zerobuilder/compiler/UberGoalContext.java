package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.ToBuilderValidator.ValidParameter;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;

import static com.google.common.base.Optional.presentInstances;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.toArray;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.GoalContext.goalTypeName;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.upcase;

final class UberGoalContext {

  static final String UPDATER_SUFFIX = "Updater";
  static final String CONTRACT = "Contract";
  static final String STEPS_IMPL = "StepsImpl";

  final GoalContext goal;
  final StepsContext stepsContext;
  final ContractContext contractContext;
  final UpdaterContext updaterContext;

  private UberGoalContext(GoalContext goal,
                          StepsContext stepsContext,
                          ContractContext contractContext,
                          UpdaterContext updaterContext) {
    this.goal = goal;
    this.stepsContext = stepsContext;
    this.contractContext = contractContext;
    this.updaterContext = updaterContext;
  }

  static UberGoalContext context(TypeName goalType, BuilderContext config,
                                 ImmutableList<ValidParameter> validParameters,
                                 ExecutableElement goal, boolean toBuilder,
                                 CodeBlock methodParameters) {
    String builderTypeName = goalName(goalType, goal) + "Builder";
    ClassName builderType = config.generatedType.nestedClass(builderTypeName);
    ClassName contractType = builderType.nestedClass(CONTRACT);
    ImmutableList<ParameterContext> parameters = parameters(contractType, goalType, validParameters);
    GoalKind kind = goalKind(goal);
    String methodName = goal.getSimpleName().toString();
    CodeBlock goalCall = goalCall(goalType, methodParameters, kind, config, methodName);
    Visibility visibility = goal.getModifiers().contains(PUBLIC)
        ? Visibility.PUBLIC
        : Visibility.PACKAGE;
    GoalContext shared = new GoalContext(goalType, builderType, config, toBuilder,
        goalName(goalType, goal),
        kind,
        visibility,
        thrownTypes(goal), parameters,
        goalCall);
    return new UberGoalContext(shared, new StepsContext(shared),
        new ContractContext(shared), new UpdaterContext(shared));
  }

  private static GoalKind goalKind(ExecutableElement goal) {
    return goal.getKind() == CONSTRUCTOR
        ? GoalKind.CONSTRUCTOR
        : goal.getModifiers().contains(STATIC)
        ? GoalKind.STATIC_METHOD
        : GoalKind.INSTANCE_METHOD;
  }

  private static String goalName(TypeName goalType, ExecutableElement goal) {
    Goal goalAnnotation = goal.getAnnotation(Goal.class);
    if (goalAnnotation == null || isNullOrEmpty(goalAnnotation.name())) {
      return goalTypeName(goalType);
    }
    return upcase(goalAnnotation.name());
  }

  static CodeBlock goalCall(TypeName goalType, CodeBlock methodParameters, GoalKind kind,
                            BuilderContext config, String methodName) {
    String returnLiteral = TypeName.VOID.equals(goalType) ? "" : "return ";
    switch (kind) {
      case CONSTRUCTOR:
        return CodeBlock.of("return new $T($L);",
            goalType, methodParameters);
      case INSTANCE_METHOD:
        return CodeBlock.of("$L$N.$N($L);",
            returnLiteral, "_" + downcase(config.annotatedType.simpleName()), methodName, methodParameters);
      case STATIC_METHOD:
        return CodeBlock.of("$L$T.$N($L);",
            returnLiteral, config.annotatedType, methodName, methodParameters);
      case FIELD:
        throw new IllegalStateException("not implemented");
      default:
        throw new IllegalStateException("unknown kind: " + kind);
    }
  }

  private static ImmutableList<ParameterContext> parameters(ClassName contract, TypeName returnType,
                                                            ImmutableList<ValidParameter> parameters) {
    ImmutableList.Builder<ParameterContext> builder = ImmutableList.builder();
    for (int i = parameters.size() - 1; i >= 0; i--) {
      ValidParameter parameter = parameters.get(i);
      ClassName stepContract = contract.nestedClass(
          upcase(parameter.name));
      builder.add(new ParameterContext(stepContract, parameter, returnType));
      returnType = stepContract;
    }
    return builder.build().reverse();
  }

  enum GoalKind {
    CONSTRUCTOR, STATIC_METHOD, INSTANCE_METHOD, FIELD
  }

  enum Visibility {
    PUBLIC, PACKAGE
  }

  TypeSpec builderImpl() {
    return classBuilder(goal.builderType)
        .addTypes(presentInstances(of(updaterContext.buildUpdaterImpl())))
        .addType(stepsContext.buildStepsImpl())
        .addType(contractContext.buildContract())
        .addModifiers(toArray(goal.maybeAddPublic(FINAL, STATIC), Modifier.class))
        .build();
  }

  private static ImmutableList<TypeName> thrownTypes(ExecutableElement goal) {
    return FluentIterable.from(goal.getThrownTypes())
        .transform(new Function<TypeMirror, TypeName>() {
          @Override
          public TypeName apply(TypeMirror thrownType) {
            return TypeName.get(thrownType);
          }
        })
        .toList();
  }

}
