package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.Analyser.AbstractGoalElement.GoalElementCases;
import net.zerobuilder.compiler.Analyser.GoalElement;
import net.zerobuilder.compiler.ToBuilderValidator.ValidParameter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.upcase;

final class GoalContextFactory {

  static GoalContext context(final GoalElement goal, final BuilderContext config,
                             ImmutableList<ValidParameter> validParameters,
                             final boolean toBuilder, final boolean builder,
                             final CodeBlock goalCall) throws ValidationException {
    String builderTypeName = goal.name + "Builder";
    final ClassName builderType = config.generatedType.nestedClass(builderTypeName);
    final ImmutableList<ParameterContext> parameters = parameters(builderType, goal.goalType, validParameters);
    final Visibility visibility = goal.element.getModifiers().contains(PUBLIC)
        ? Visibility.PUBLIC
        : Visibility.PACKAGE;
    return goal.accept(new GoalElementCases<GoalContext>() {
      @Override
      public GoalContext executable(ExecutableElement element, GoalKind kind) throws ValidationException {
        return new GoalContext.RegularGoalContext(
            goal.goalType,
            config,
            toBuilder,
            builder,
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
            config,
            toBuilder,
            builder,
            goal.name,
            parameters,
            goalCall);
      }
    });
  }

  private static ImmutableList<ParameterContext> parameters(ClassName builderType, TypeName returnType,
                                                            ImmutableList<ValidParameter> parameters) {
    ImmutableList.Builder<ParameterContext> builder = ImmutableList.builder();
    for (int i = parameters.size() - 1; i >= 0; i--) {
      ValidParameter parameter = parameters.get(i);
      ClassName stepContract = builderType.nestedClass(
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
