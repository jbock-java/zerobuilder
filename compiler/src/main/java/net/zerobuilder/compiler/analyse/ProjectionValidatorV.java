package net.zerobuilder.compiler.analyse;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoGoalElement.RegularGoalElement;
import net.zerobuilder.compiler.analyse.ProjectionValidator.TmpRegularParameter;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;
import net.zerobuilder.compiler.generate.DtoGoalDescription.RegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoParameter;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import static com.google.auto.common.MoreElements.getLocalAndInheritedMethods;
import static com.google.auto.common.MoreTypes.asTypeElement;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static net.zerobuilder.compiler.Messages.ErrorMessages.NO_PROJECTION;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.TmpRegularParameter.toValidParameter;
import static net.zerobuilder.compiler.analyse.ProjectionValidator.shuffledParameters;
import static net.zerobuilder.compiler.analyse.Utilities.upcase;

final class ProjectionValidatorV {

  static final Function<RegularGoalElement, GoalDescription> validateValue
      = goal -> {
    TypeElement type = asTypeElement(goal.executableElement.getEnclosingElement().asType());
    ImmutableMap<String, ExecutableElement> methods = methods(goal, type);
    ImmutableMap<String, VariableElement> fields = fields(type);
    ImmutableList.Builder<TmpRegularParameter> builder = ImmutableList.builder();
    for (VariableElement parameter : goal.executableElement.getParameters()) {
      VariableElement field = fields.get(parameter.getSimpleName().toString());
      if (field != null && TypeName.get(field.asType()).equals(TypeName.get(parameter.asType()))) {
        builder.add(TmpRegularParameter.create(parameter, Optional.absent(), goal.goalAnnotation));
      } else {
        String methodName = "get" + upcase(parameter.getSimpleName().toString());
        ExecutableElement method = methods.get(methodName);
        if (method == null
            && TypeName.get(parameter.asType()) == TypeName.BOOLEAN) {
          methodName = "is" + upcase(parameter.getSimpleName().toString());
          method = methods.get(methodName);
        }
        if (method == null) {
          methodName = parameter.getSimpleName().toString();
          method = methods.get(methodName);
        }
        if (method == null) {
          throw new ValidationException(NO_PROJECTION, parameter);
        }
        builder.add(TmpRegularParameter.create(parameter, Optional.of(methodName), goal.goalAnnotation));
      }
    }
    return createResult(goal, builder.build());
  };
  private static ImmutableMap<String, VariableElement> fields(TypeElement type) {
    return FluentIterable.from(fieldsIn(type.getEnclosedElements()))
        .filter(field -> !field.getModifiers().contains(PRIVATE)
            && !field.getModifiers().contains(STATIC))
        .uniqueIndex(field -> field.getSimpleName().toString());
  }
  private static ImmutableMap<String, ExecutableElement> methods(RegularGoalElement goal, TypeElement type) {
    return FluentIterable.from(getLocalAndInheritedMethods(type, goal.elements))
        .filter(method -> method.getParameters().isEmpty()
            && !method.getModifiers().contains(PRIVATE)
            && !method.getModifiers().contains(STATIC))
        .uniqueIndex(method -> method.getSimpleName().toString());
  }

  static final Function<RegularGoalElement, GoalDescription> validateValueSkipProjections
      = goal -> {
    ImmutableList.Builder<TmpRegularParameter> builder = ImmutableList.builder();
    for (VariableElement parameter : goal.executableElement.getParameters()) {
      builder.add(TmpRegularParameter.create(parameter, Optional.absent(), goal.goalAnnotation));
    }
    return createResult(goal, builder.build());
  };

  private static GoalDescription createResult(RegularGoalElement goal, ImmutableList<TmpRegularParameter> parameters) {
    ImmutableList<TmpRegularParameter> shuffled = shuffledParameters(parameters);
    return create(goal, FluentIterable.from(shuffled).transform(toValidParameter).toList());
  }

  private static RegularGoalDescription create(RegularGoalElement goal, ImmutableList<DtoParameter.RegularParameter> parameters) {
    ImmutableList<TypeName> thrownTypes = thrownTypes(goal.executableElement);
    return RegularGoalDescription.create(goal.details, thrownTypes, parameters);
  }

  private static ImmutableList<TypeName> thrownTypes(ExecutableElement executableElement) {
    return FluentIterable
        .from(executableElement.getThrownTypes())
        .transform((Function<TypeMirror, TypeName>) thrownType -> TypeName.get(thrownType))
        .toList();
  }

  private ProjectionValidatorV() {
    throw new UnsupportedOperationException("no instances");
  }
}
