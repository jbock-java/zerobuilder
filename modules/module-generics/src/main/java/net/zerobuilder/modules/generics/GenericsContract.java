package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularParameter;

import java.util.ArrayList;
import java.util.List;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.context;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class GenericsContract {

  static List<TypeSpec> stepInterfaces(SimpleStaticMethodGoalContext goal,
                                       List<List<TypeVariableName>> typeParams,
                                       List<List<TypeVariableName>> methodParams) {
    ArrayList<TypeSpec> builder = new ArrayList<>();
    for (int i = 0; i < goal.parameters.size(); i++) {
      DtoRegularParameter.SimpleParameter parameter = goal.parameters.get(i);
      builder.add(TypeSpec.interfaceBuilder(upcase(parameter.name))
          .addTypeVariables(typeParams.get(i))
          .addMethod(nextStep(goal,
              typeParams,
              methodParams, i))
          .addModifiers(PUBLIC)
          .build());
    }
    return builder;
  }

  private static MethodSpec nextStep(SimpleStaticMethodGoalContext goal, List<List<TypeVariableName>> typeParams, List<List<TypeVariableName>> methodParams, int i) {
    DtoRegularParameter.SimpleParameter parameter = goal.parameters.get(i);
    return MethodSpec.methodBuilder(downcase(parameter.name))
        .addTypeVariables(methodParams.get(i))
        .addModifiers(PUBLIC, ABSTRACT)
        .returns(nextStepType(goal, typeParams, i))
        .addParameter(parameterSpec(parameter.type, parameter.name))
        .build();
  }

  private static TypeName nextStepType(SimpleStaticMethodGoalContext goal,
                                       List<List<TypeVariableName>> typeParams,
                                       int i) {
    if (i == typeParams.size() - 1) {
      return goal.details.goalType;
    }
    ClassName rawNext = goal.context.generatedType
        .nestedClass(upcase(goal.details.name() + "Builder"))
        .nestedClass(upcase(goal.parameters.get(i + 1).name));
    return parameterizedTypeName(rawNext, typeParams.get(i + 1));
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
    List<TypeName> builder = new ArrayList<>(goal.parameters.size() + 1);
    goal.parameters.stream().map(step -> step.type).forEach(builder::add);
    builder.add(goal.details.goalType);
    return builder;
  }
}