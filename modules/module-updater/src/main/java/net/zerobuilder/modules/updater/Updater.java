package net.zerobuilder.modules.updater;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeName.BOOLEAN;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoGoalDetails.isInstance;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.modules.updater.RegularUpdater.implType;
import static net.zerobuilder.modules.updater.RegularUpdater.isReusable;

final class Updater {

  static List<FieldSpec> fields(ProjectedRegularGoalDescription description) {
    List<FieldSpec> builder = new ArrayList<>();
    if (isReusable.apply(description.details)) {
      builder.add(fieldSpec(BOOLEAN, "_currently_in_use", PRIVATE));
    }
    if (isInstance.apply(description.details)) {
      builder.add(fieldSpec(description.context.type, "_factory", PRIVATE));
    }
    for (ProjectedParameter step : description.parameters) {
      String name = step.name;
      TypeName type = step.type;
      builder.add(fieldSpec(type, name, PRIVATE));
    }
    return builder;
  }

  static List<MethodSpec> stepMethods(ProjectedRegularGoalDescription description) {
    return description.parameters.stream()
        .map(updateMethods(description))
        .collect(toList());
  }

  private static Function<ProjectedParameter, MethodSpec> updateMethods(ProjectedRegularGoalDescription description) {
    return step -> normalUpdate(description, step);
  }

  private static MethodSpec normalUpdate(ProjectedRegularGoalDescription description, ProjectedParameter step) {
    String name = step.name;
    TypeName type = step.type;
    ParameterSpec parameter = parameterSpec(type, name);
    return methodBuilder(name)
        .returns(implType(description))
        .addParameter(parameter)
        .addStatement("this.$N = $N", fieldSpec(step.type, step.name), parameter)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private Updater() {
    throw new UnsupportedOperationException("no instances");
  }
}
