package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class GenericsContract {

  static List<TypeSpec> stepInterfaces(SimpleRegularGoalDescription description,
                                       List<List<TypeVariableName>> typeParams,
                                       List<List<TypeVariableName>> methodParams) {
    List<TypeSpec> builder = new ArrayList<>(description.parameters.size());
    List<SimpleParameter> steps = description.parameters;
    for (int i = 0; i < steps.size(); i++) {
      SimpleParameter parameter = steps.get(i);
      builder.add(TypeSpec.interfaceBuilder(upcase(parameter.name))
          .addTypeVariables(typeParams.get(i))
          .addMethod(nextStep(description,
              typeParams,
              methodParams, i))
          .addModifiers(PUBLIC)
          .build());
    }
    return builder;
  }

  private static MethodSpec nextStep(SimpleRegularGoalDescription description, List<List<TypeVariableName>> typeParams, List<List<TypeVariableName>> methodParams, int i) {
    List<SimpleParameter> steps = description.parameters;
    SimpleParameter parameter = steps.get(i);
    return MethodSpec.methodBuilder(downcase(parameter.name))
        .addTypeVariables(methodParams.get(i))
        .addModifiers(PUBLIC, ABSTRACT)
        .returns(nextStepType(description, typeParams, i))
        .addExceptions(i == description.parameters.size() - 1 ?
            description.thrownTypes :
            emptyList())
        .addParameter(parameterSpec(parameter.type, parameter.name))
        .build();
  }

  private static TypeName nextStepType(SimpleRegularGoalDescription description,
                                       List<List<TypeVariableName>> typeParams,
                                       int i) {
    if (i == description.parameters.size() - 1) {
      return description.details.type();
    }
    List<SimpleParameter> steps = description.parameters;
    SimpleParameter step = steps.get(i + 1);
    ClassName rawNext = description.context.generatedType
        .nestedClass(upcase(description.details.name() + "Builder"))
        .nestedClass(upcase(step.name));
    return parameterizedTypeName(rawNext, typeParams.get(i + 1));
  }

  static ClassName contractType(SimpleRegularGoalDescription description) {
    String contractName = upcase(description.details.name) + "Builder";
    return description.context
        .generatedType.nestedClass(contractName);
  }

  static ClassName implType(SimpleRegularGoalDescription description) {
    String contractName = upcase(description.details.name) + "BuilderImpl";
    return description.context.generatedType.nestedClass(contractName);
  }

  static List<TypeName> stepTypes(SimpleRegularGoalDescription description) {
    List<TypeName> builder = new ArrayList<>(description.parameters.size() + 1);
    description.parameters.stream().map(step -> step.type)
        .forEach(builder::add);
    builder.add(description.details.type());
    return builder;
  }
}