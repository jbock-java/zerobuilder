package net.zerobuilder.modules.generics;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import java.util.List;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class ImplFields {

  private final ClassName impl;
  private final BuilderGoalDescription description;
  private final List<List<TypeVariableName>> typeParams;

  ImplFields(
      ClassName impl,
      BuilderGoalDescription description,
      List<List<TypeVariableName>> typeParams) {
    this.impl = impl;
    this.description = description;
    this.typeParams = typeParams;
  }

  List<FieldSpec> fields(int i) {
    return i == 0 ?
        List.of() :
        normalFields(i);
  }

  private List<FieldSpec> normalFields(int i) {
    TypeName implType = parameterizedTypeName(
        impl.nestedClass(upcase(description.parameters().get(i - 1).name())),
        typeParams.get(i - 1));
    return List.of(
        FieldSpec.builder(implType, description.parameters().get(i - 1).name() + "Acc",
            PRIVATE, FINAL).build(),
        FieldSpec.builder(description.parameters().get(i - 1).type(), description.parameters().get(i - 1).name(),
            PRIVATE, FINAL).build());
  }
}
