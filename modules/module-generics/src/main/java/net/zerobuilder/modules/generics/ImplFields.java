package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoal;

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

final class ImplFields {

  private final ClassName impl;
  private final DtoRegularGoal.SimpleRegularGoalContext goal;
  private final List<TypeSpec> stepSpecs;
  private final List<List<TypeVariableName>> typeParams;

  ImplFields(ClassName impl,
             DtoRegularGoal.SimpleRegularGoalContext goal,
             List<TypeSpec> stepSpecs,
             List<List<TypeVariableName>> typeParams) {
    this.impl = impl;
    this.goal = goal;
    this.stepSpecs = stepSpecs;
    this.typeParams = typeParams;
  }

  final BiFunction<DtoGoalDetails.AbstractRegularDetails, Integer, List<FieldSpec>> fields = fields();

  private BiFunction<DtoGoalDetails.AbstractRegularDetails, Integer, List<FieldSpec>> fields() {
    return regularDetailsCases(
        (constructor, i) -> i == 0 ?
            emptyList() :
            normalFields(i),
        (staticMethod, i) -> i == 0 ?
            emptyList() :
            normalFields(i),
        (instanceMethod, i) -> i == 0 ?
            singletonList(FieldSpec.builder(goal.context().type, "instance",
                PRIVATE, FINAL).build()) :
            normalFields(i)
    );
  }

  private List<FieldSpec> normalFields(int i) {
    TypeSpec stepSpec = stepSpecs.get(i - 1);
    TypeName implType = parameterizedTypeName(impl.nestedClass(stepSpec.name + "Impl"),
        typeParams.get(i - 1));
    return asList(
        FieldSpec.builder(implType, downcase(stepSpec.name) + "Impl",
            PRIVATE, FINAL)
            .build(),
        parameterField(stepSpec));
  }

  private FieldSpec parameterField(TypeSpec type) {
    MethodSpec method = type.methodSpecs.get(0);
    ParameterSpec parameter = method.parameters.get(0);
    return FieldSpec.builder(parameter.type, parameter.name, PRIVATE, FINAL)
        .build();
  }
}
