package net.zerobuilder.modules.updater;

import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import java.util.List;
import net.zerobuilder.compiler.generate.GoalDescription;
import net.zerobuilder.compiler.generate.ProjectedParameter;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.modules.updater.RegularUpdater.implType;

final class Updater {

  Updater() {
  }

  static List<FieldSpec> fields(GoalDescription description) {
    return description.parameters().stream()
        .map(step -> fieldSpec(step.type(), step.name(), PRIVATE))
        .toList();
  }

  static List<MethodSpec> stepMethods(GoalDescription description) {
    return description.parameters().stream()
        .map(step -> normalUpdate(description, step))
        .toList();
  }

  private static MethodSpec normalUpdate(
      GoalDescription description,
      ProjectedParameter step) {
    String name = step.name();
    TypeName type = step.type();
    ParameterSpec parameter = parameterSpec(type, name);
    return methodBuilder(name)
        .returns(implType(description))
        .addParameter(parameter)
        .addStatement("this.$N = $N", fieldSpec(step.type(), step.name()), parameter)
        .addStatement("return this")
        .addModifiers(description.details().getAccess())
        .build();
  }
}
