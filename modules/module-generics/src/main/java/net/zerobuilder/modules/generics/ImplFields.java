package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import java.util.List;
import java.util.function.BiFunction;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoGoalDetails.regularDetailsCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class ImplFields {

  private final ClassName impl;
  private final SimpleRegularGoalDescription description;
  private final List<List<TypeVariableName>> typeParams;

  ImplFields(ClassName impl,
             SimpleRegularGoalDescription description,
             List<List<TypeVariableName>> typeParams) {
    this.impl = impl;
    this.description = description;
    this.typeParams = typeParams;
  }

  final BiFunction<AbstractRegularDetails, Integer, List<FieldSpec>> fields = fields();

  private BiFunction<AbstractRegularDetails, Integer, List<FieldSpec>> fields() {
    return regularDetailsCases(
        (constructor, i) -> i == 0 ?
            emptyList() :
            normalFields(i),
        (staticMethod, i) -> i == 0 ?
            emptyList() :
            normalFields(i),
        (instanceMethod, i) -> i == 0 ?
            singletonList(FieldSpec.builder(description.context.type, "instance",
                PRIVATE, FINAL).build()) :
            normalFields(i)
    );
  }

  private List<FieldSpec> normalFields(int i) {
    String name = upcase(description.parameters.get(i - 1).name);
    TypeName implType = parameterizedTypeName(impl.nestedClass(name),
        typeParams.get(i - 1));
    return asList(
        FieldSpec.builder(implType, downcase(name), PRIVATE, FINAL).build(),
        FieldSpec.builder(description.parameters.get(i).type, description.parameters.get(i).name).build());
  }

  private FieldSpec parameterField(TypeSpec type) {
    MethodSpec method = type.methodSpecs.get(0);
    ParameterSpec parameter = method.parameters.get(0);
    return FieldSpec.builder(parameter.type, parameter.name, PRIVATE, FINAL)
        .build();
  }

/*
  static List<TypeSpec> stepInterfaces(SimpleRegularGoalDescription description,
                                       List<List<TypeVariableName>> typeParams,
                                       List<List<TypeVariableName>> methodParams) {
    List<TypeSpec> builder = new ArrayList<>(description.parameters.size());
    List<DtoRegularParameter.SimpleParameter> steps = description.parameters;
    for (int i = 0; i < steps.size(); i++) {
      DtoRegularParameter.SimpleParameter parameter = steps.get(i);
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

*/
}
