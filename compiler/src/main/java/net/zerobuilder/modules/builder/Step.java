package net.zerobuilder.modules.builder;

import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.jbock.simple.Inject;
import net.zerobuilder.compiler.generate.GoalDescription;
import net.zerobuilder.compiler.generate.ProjectedParameter;

import java.util.List;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static com.palantir.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;

record Step(
    GoalDescription description,
    Builder builder,
    BuilderUtil util) {

  @Inject
  Step {
  }

  TypeSpec stepInterface(int i) {
    return interfaceBuilder(util.stepType(i))
        .addTypeVariables(description.details().instanceTypeParameters())
        .addMethod(stepMethod(i))
        .addModifiers(PUBLIC)
        .build();
  }

  private MethodSpec stepMethod(int i) {
    ProjectedParameter parameter = description.parameters().get(i);
    String name = parameter.stepName();
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
