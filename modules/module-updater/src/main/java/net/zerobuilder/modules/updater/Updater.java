package net.zerobuilder.modules.updater;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.ZeroUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeName.BOOLEAN;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.modules.updater.RegularUpdater.implType;

final class Updater {

  static List<FieldSpec> fields(ProjectedRegularGoalContext goal) {
    List<FieldSpec> builder = new ArrayList<>();
    if (goal.mayReuse()) {
      builder.add(fieldSpec(BOOLEAN, "_currently_in_use", PRIVATE));
    }
    if (goal.isInstance()) {
      builder.add(fieldSpec(goal.context().type, "_factory", PRIVATE));
    }
    for (ProjectedParameter step : goal.description().parameters()) {
      String name = step.name;
      TypeName type = step.type;
      builder.add(fieldSpec(type, name, PRIVATE));
    }
    return builder;
  }

  static List<MethodSpec> stepMethods(ProjectedRegularGoalContext goal) {
    return goal.description().parameters().stream()
        .map(updateMethods(goal))
        .collect(toList());
  }

  private static Function<ProjectedParameter, MethodSpec> updateMethods(ProjectedRegularGoalContext goal) {
    return step -> normalUpdate(goal, step);
  }

  private static MethodSpec normalUpdate(ProjectedRegularGoalContext goal, ProjectedParameter step) {
    String name = step.name;
    TypeName type = step.type;
    ParameterSpec parameter = parameterSpec(type, name);
    return methodBuilder(name)
        .returns(implType(goal))
        .addParameter(parameter)
        .addCode(nullCheck(step))
        .addStatement("this.$N = $N", fieldSpec(step.type, step.name), parameter)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static CodeBlock nullCheck(ProjectedParameter parameter) {
    if (!parameter.nullPolicy.check() || parameter.type.isPrimitive()) {
      return emptyCodeBlock;
    }
    String name = parameter.name;
    return ZeroUtil.nullCheck(name, name);
  }

  private Updater() {
    throw new UnsupportedOperationException("no instances");
  }
}
