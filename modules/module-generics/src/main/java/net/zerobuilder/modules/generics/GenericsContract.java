package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoRegularGoal;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.context;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class GenericsContract {

  static List<TypeSpec> stepInterfaces(SimpleRegularGoalContext goal,
                                       List<List<TypeVariableName>> typeParams,
                                       List<List<TypeVariableName>> methodParams) {
    List<TypeSpec> builder = new ArrayList<>();
    List<SimpleParameter> steps = goal.description().parameters();
    for (int i = 0; i < steps.size(); i++) {
      SimpleParameter parameter = steps.get(i);
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

  private static MethodSpec nextStep(SimpleRegularGoalContext goal, List<List<TypeVariableName>> typeParams, List<List<TypeVariableName>> methodParams, int i) {
    List<SimpleParameter> steps = goal.description().parameters();
    SimpleParameter parameter = steps.get(i);
    return MethodSpec.methodBuilder(downcase(parameter.name))
        .addTypeVariables(methodParams.get(i))
        .addModifiers(PUBLIC, ABSTRACT)
        .returns(nextStepType(goal, typeParams, i))
        .addExceptions(i == goal.description().parameters().size() - 1 ?
            goal.thrownTypes :
            emptyList())
        .addParameter(parameterSpec(parameter.type, parameter.name))
        .build();
  }

  private static TypeName nextStepType(SimpleRegularGoalContext goal,
                                       List<List<TypeVariableName>> typeParams,
                                       int i) {
    if (i == goal.description().parameters().size() - 1) {
      return goal.regularDetails().type();
    }
    List<SimpleParameter> steps = goal.description().parameters();
    SimpleParameter step = steps.get(i + 1);
    ClassName rawNext = goal.context().generatedType
        .nestedClass(upcase(goal.regularDetails().name() + "Builder"))
        .nestedClass(upcase(step.name));
    return parameterizedTypeName(rawNext, typeParams.get(i + 1));
  }

  static ClassName contractType(SimpleRegularGoalContext goal) {
    String contractName = upcase(goal.regularDetails().name) + "Builder";
    return context.apply(goal)
        .generatedType.nestedClass(contractName);
  }

  static ClassName implType(SimpleRegularGoalContext goal) {
    String contractName = upcase(goal.regularDetails().name) + "BuilderImpl";
    return context.apply(goal)
        .generatedType.nestedClass(contractName);
  }

  static List<TypeName> stepTypes(SimpleRegularGoalContext goal) {
    List<TypeName> builder = new ArrayList<>(goal.description().parameters().size() + 1);
    DtoRegularGoal.stepTypes.apply(goal).forEach(builder::add);
    builder.add(goal.regularDetails().type());
    return builder;
  }
}