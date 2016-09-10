package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.Analyser.AbstractGoalElement.Cases;
import net.zerobuilder.compiler.Analyser.GoalElement;
import net.zerobuilder.compiler.ToBuilderValidator.ValidParameter;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

import static com.google.common.base.Optional.presentInstances;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.toArray;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
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

  static UberGoalContext context(GoalElement namedGoal, BuilderContext config,
                                 ImmutableList<ValidParameter> validParameters,
                                 boolean toBuilder, CodeBlock goalCall) throws ValidationException {
    String builderTypeName = namedGoal.name + "Builder";
    ClassName builderType = config.generatedType.nestedClass(builderTypeName);
    ClassName contractType = builderType.nestedClass(CONTRACT);
    ImmutableList<ParameterContext> parameters = parameters(contractType, namedGoal.goalType, validParameters);
    Optional<GoalKind> kind = namedGoal.goalKind();
    Visibility visibility = namedGoal.element.getModifiers().contains(PUBLIC)
        ? Visibility.PUBLIC
        : Visibility.PACKAGE;
    GoalContext shared = new GoalContext(namedGoal.goalType, builderType, config, toBuilder,
        namedGoal.name,
        kind,
        visibility,
        thrownTypes(namedGoal), parameters,
        goalCall);
    return new UberGoalContext(shared, new StepsContext(shared),
        new ContractContext(shared), new UpdaterContext(shared));
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

  // field goals don't have a kind
  enum GoalKind {
    CONSTRUCTOR, STATIC_METHOD, INSTANCE_METHOD
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

  private static ImmutableList<TypeName> thrownTypes(GoalElement goal) throws ValidationException {
    return FluentIterable
        .from(goal.accept(new Cases<List<? extends TypeMirror>>() {
          @Override
          public List<? extends TypeMirror> executable(ExecutableElement element, GoalKind kind) throws ValidationException {
            return element.getThrownTypes();
          }
          @Override
          public List<? extends TypeMirror> field(VariableElement field) throws ValidationException {
            return ImmutableList.of();
          }
        }))
        .transform(new Function<TypeMirror, TypeName>() {
          @Override
          public TypeName apply(TypeMirror thrownType) {
            return TypeName.get(thrownType);
          }
        })
        .toList();
  }

}
