package net.zerobuilder.modules.generics;

import io.jbock.javapoet.ClassName;
import io.jbock.javapoet.FieldSpec;
import io.jbock.javapoet.TypeName;
import io.jbock.javapoet.TypeVariableName;
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
            normalFields(i));
  }

  private List<FieldSpec> normalFields(int i) {
    TypeName implType = parameterizedTypeName(
        impl.nestedClass(upcase(description.parameters.get(i - 1).name)),
        typeParams.get(i - 1));
    return asList(
        FieldSpec.builder(implType, description.parameters.get(i - 1).name + "Acc",
            PRIVATE, FINAL).build(),
        FieldSpec.builder(description.parameters.get(i - 1).type, description.parameters.get(i - 1).name,
            PRIVATE, FINAL).build());
  }
}
