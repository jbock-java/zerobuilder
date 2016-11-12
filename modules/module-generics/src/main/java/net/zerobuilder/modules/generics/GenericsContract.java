package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularStep.SimpleRegularStep;
import net.zerobuilder.compiler.generate.DtoStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.context;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class GenericsContract {

  static final TypeVariableName[] NO_TYPEVARNAME = new TypeVariableName[0];

  private static TypeName nextType(DtoStep.AbstractStep step) {
    if (step.nextStep.isPresent()) {
      return step.context.generatedType
          .nestedClass(upcase(step.goalDetails.name() + "Builder"))
          .nestedClass(step.nextStep.get().thisType);
    }
    return step.goalDetails.type();
  }

  static List<TypeSpec> stepInterfaces(SimpleStaticMethodGoalContext goal,
                                       List<List<TypeVariableName>> typeParams,
                                       List<List<TypeVariableName>> methodParams) {
    ArrayList<TypeSpec> builder = new ArrayList<>();
    for (int i = 0; i < goal.steps.size(); i++) {
      SimpleRegularStep step = goal.steps.get(i);
      builder.add(TypeSpec.interfaceBuilder(step.thisType)
          .addTypeVariables(typeParams.get(i))
          .addMethod(nextStep(methodParams.get(i), step))
          .build());
    }
    return builder;
  }

  private static MethodSpec nextStep(List<TypeVariableName> typeVars, SimpleRegularStep step) {
    return MethodSpec.methodBuilder(downcase(step.thisType))
        .addTypeVariables(typeVars)
        .addModifiers(PUBLIC, ABSTRACT)
        .returns(nextStepType(typeVars, step))
        .addParameter(parameterSpec(step.parameter.type, step.parameter.name))
        .build();
  }

  private static TypeName nextStepType(List<TypeVariableName> typeVars, SimpleRegularStep step) {
    if (step.isLast()) {
      return nextType(step);
    }
    Optional<ClassName> rawClassName = rawClassName(nextType(step));
    return rawClassName.isPresent() ?
        typeVars.isEmpty() ?
            rawClassName.get() :
            ParameterizedTypeName.get(rawClassName.get(),
                typeVars.toArray(NO_TYPEVARNAME)) :
        nextType(step);
  }

  static ClassName contractType(SimpleStaticMethodGoalContext goal) {
    String contractName = upcase(goal.details.name) + "Builder";
    return context.apply(goal)
        .generatedType.nestedClass(contractName);
  }

  static ClassName implType(SimpleStaticMethodGoalContext goal) {
    String contractName = upcase(goal.details.name) + "BuilderImpl";
    return context.apply(goal)
        .generatedType.nestedClass(contractName);
  }

  static List<TypeName> stepTypes(SimpleStaticMethodGoalContext goal) {
    List<TypeName> builder = new ArrayList<>(goal.steps.size() + 1);
    goal.steps.stream().map(step -> step.parameter.type).forEach(builder::add);
    builder.add(goal.details.goalType);
    return builder;
  }
}