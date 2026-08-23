package net.zerobuilder.modules.updater;

import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.UpdaterGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.modules.updater.RegularUpdater.implType;

final class Updater {

  static List<FieldSpec> fields(UpdaterGoalDescription description) {
    List<FieldSpec> builder = new ArrayList<>(description.parameters().size());
    for (ProjectedParameter step : description.parameters()) {
      String name = step.name();
      TypeName type = step.type();
      builder.add(fieldSpec(type, name, PRIVATE));
    }
    return builder;
  }

  static List<MethodSpec> stepMethods(UpdaterGoalDescription description) {
    return description.parameters().stream()
        .map(step -> normalUpdate(description, step))
        .collect(toList());
  }

  private static MethodSpec normalUpdate(
      UpdaterGoalDescription description,
      ProjectedParameter step) {
    String name = step.name();
    TypeName type = step.type();
    ParameterSpec parameter = parameterSpec(type, name);
    return methodBuilder(name)
        .returns(implType(description))
        .addParameter(parameter)
        .addStatement("this.$N = $N", fieldSpec(step.type(), step.name()), parameter)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private Updater() {
    throw new UnsupportedOperationException("no instances");
  }
}
