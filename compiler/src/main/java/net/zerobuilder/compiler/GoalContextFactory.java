package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.Analyser.AbstractGoalElement.GoalElementCases;
import net.zerobuilder.compiler.Analyser.GoalElement;
import net.zerobuilder.compiler.GoalContext.GoalCases;
import net.zerobuilder.compiler.ToBuilderValidator.ValidParameter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
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
import static net.zerobuilder.compiler.GoalContext.always;
import static net.zerobuilder.compiler.Utilities.upcase;

final class GoalContextFactory {

  static final String UPDATER_SUFFIX = "Updater";
  static final String CONTRACT = "Contract";
  static final String STEPS_IMPL = "StepsImpl";

  static GoalContext context(final GoalElement goal, final BuilderContext config,
                             ImmutableList<ValidParameter> validParameters,
                             final boolean toBuilder, final CodeBlock goalCall) throws ValidationException {
    String builderTypeName = goal.name + "Builder";
    final ClassName builderType = config.generatedType.nestedClass(builderTypeName);
    ClassName contractType = builderType.nestedClass(CONTRACT);
    final ImmutableList<ParameterContext> parameters = parameters(contractType, goal.goalType, validParameters);
    final Visibility visibility = goal.element.getModifiers().contains(PUBLIC)
        ? Visibility.PUBLIC
        : Visibility.PACKAGE;
    return goal.accept(new GoalElementCases<GoalContext>() {
      @Override
      public GoalContext executable(ExecutableElement element, GoalKind kind) throws ValidationException {
        return new GoalContext.RegularGoalContext(
            goal.goalType,
            builderType,
            config,
            toBuilder,
            kind,
            goal.name,
            visibility,
            thrownTypes(goal),
            parameters,
            goalCall);
      }
      @Override
      public GoalContext field(Element field, TypeElement typeElement) throws ValidationException {
        return new GoalContext.FieldGoalContext(
            (ClassName) goal.goalType,
            builderType,
            config,
            toBuilder,
            goal.name,
            parameters,
            goalCall);
      }
    });
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

  static GoalCases<TypeSpec> builderImpl = always(new Function<GoalContext, TypeSpec>() {
    @Override
    public TypeSpec apply(GoalContext goal) {
      return classBuilder(goal.generatedType)
          .addTypes(presentInstances(of(UpdaterContext.buildUpdaterImpl(goal))))
          .addType(StepsContext.buildStepsImpl(goal))
          .addType(ContractContext.buildContract(goal))
          .addModifiers(toArray(goal.maybeAddPublic(FINAL, STATIC), Modifier.class))
          .build();
    }
  });

  private static ImmutableList<TypeName> thrownTypes(GoalElement goal) throws ValidationException {
    return FluentIterable
        .from(goal.accept(new GoalElementCases<List<? extends TypeMirror>>() {
          @Override
          public List<? extends TypeMirror> executable(ExecutableElement element, GoalKind kind) throws ValidationException {
            return element.getThrownTypes();
          }
          @Override
          public List<? extends TypeMirror> field(Element field, TypeElement typeElement) throws ValidationException {
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

  private GoalContextFactory() {
    throw new UnsupportedOperationException("no instances");
  }
}
