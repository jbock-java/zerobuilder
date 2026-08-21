package net.zerobuilder.modules.generics;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import java.util.List;
import net.zerobuilder.compiler.generate.DtoGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import static java.util.Arrays.asList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
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

  List<FieldSpec> fields(AbstractRegularDetails details, int i) {
    return switch (details) {
      case DtoGoalDetails.ConstructorGoalDetails constructor -> i == 0 ?
          List.of() :
          normalFields(i);
      case DtoGoalDetails.StaticMethodGoalDetails staticMethod -> i == 0 ?
          List.of() :
          normalFields(i);
      case DtoGoalDetails.InstanceMethodGoalDetails instanceMethod -> i == 0 ?
          List.of(FieldSpec.builder(description.context.type, "instance",
              PRIVATE, FINAL).build()) :
          normalFields(i);
    };
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
