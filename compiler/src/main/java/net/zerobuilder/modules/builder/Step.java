package net.zerobuilder.modules.builder;

import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.jbock.simple.Inject;
import java.util.List;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static com.palantir.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;

final class Step {

  private final BuilderGoalDescription description;
  private final Builder builder;
  private final BuilderUtil util;

  @Inject
  Step(BuilderGoalDescription description, Builder builder, BuilderUtil util) {
    this.description = description;
    this.builder = builder;
    this.util = util;
  }

  TypeSpec stepInterface(int i) {
    return interfaceBuilder(util.stepInterfaceName(i))
        .addMethod(stepMethod(i))
        .addModifiers(PUBLIC)
        .build();
  }

  private MethodSpec stepMethod(int i) {
    SimpleParameter parameter = description.parameters().get(i);
    String name = parameter.name();
    TypeName type = parameter.type();
    List<TypeName> thrownTypes = i == description.parameters().size() - 1 ?
        description.thrownTypes() :
        List.of();
    return methodBuilder(name)
        .returns(builder.nextType(i))
        .addParameter(parameterSpec(type, name))
        .addExceptions(thrownTypes)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }
}
