package net.zerobuilder.modules.updater;

import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import io.jbock.simple.Inject;
import net.zerobuilder.compiler.generate.GoalDescription;
import net.zerobuilder.compiler.generate.ProjectedParameter;

import java.util.List;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;

record Updater(GoalDescription description) {

  @Inject
  Updater {
  }

  List<FieldSpec> fields() {
    return description.parameters().stream()
        .map(step -> fieldSpec(step.type(), step.name(), PRIVATE))
        .toList();
  }

  List<MethodSpec> stepMethods() {
    return description.parameters().stream()
        .map(this::normalUpdate)
        .toList();
  }

  TypeName implType() {
    return parameterizedTypeName(
        description.generatedType().nestedClass(implTypeName()),
        description.details().instanceTypeParameters());
  }

  private String implTypeName() {
    return description.details().tel().getSimpleName() + "Updater";
  }

  private MethodSpec normalUpdate(
      ProjectedParameter step) {
    String name = step.name();
    TypeName type = step.type();
    ParameterSpec parameter = parameterSpec(type, name);
    return methodBuilder(name)
        .returns(implType())
        .addParameter(parameter)
        .addStatement("this.$N = $N", fieldSpec(step.type(), step.name()), parameter)
        .addStatement("return this")
        .addModifiers(description.details().getAccess())
        .build();
  }
}
