package net.zerobuilder.modules.builder;

import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static com.palantir.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.Builder.nextType;

final class Step {

  static TypeSpec stepInterface(BuilderGoalDescription description, int i) {
    return interfaceBuilder(upcase(description.parameters().get(i).name()))
        .addMethod(stepMethod(i, description))
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec stepMethod(
      int i,
      BuilderGoalDescription description) {
    SimpleParameter parameter = description.parameters().get(i);
    String name = parameter.name();
    TypeName type = parameter.type();
    List<TypeName> thrownTypes = i == description.parameters().size() - 1 ?
        description.thrownTypes() :
        List.of();
    return methodBuilder(name)
        .returns(nextType(i, description))
        .addParameter(parameterSpec(type, name))
        .addExceptions(thrownTypes)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  private Step() {
    throw new UnsupportedOperationException("no instances");
  }
}
