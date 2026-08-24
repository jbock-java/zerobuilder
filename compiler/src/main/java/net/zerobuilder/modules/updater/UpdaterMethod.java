package net.zerobuilder.modules.updater;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoProjectionInfo;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.FieldAccess;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.GetterMethod;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.UpdaterGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.GoalDetails;
import net.zerobuilder.compiler.generate.ZeroUtil;

import java.util.List;
import java.util.Set;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static java.util.stream.Collectors.toSet;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.modules.updater.RegularUpdater.implType;

final class UpdaterMethod {

  static MethodSpec updaterMethod(
      UpdaterGoalDescription description) {
    ParameterSpec updater = varUpdater(description);
    return methodBuilder("builder")
        .addExceptions(thrownByProjections(description))
        .addParameter(toBuilderParameter(description))
        .addTypeVariables(description.details().instanceTypeParameters())
        .returns(updater.type())
        .addCode(initVarUpdater(updater))
        .addCode(copyBlock(description))
        .addStatement("return $N", updater)
        .addModifiers(description.details().access(STATIC))
        .build();
  }

  private static CodeBlock copyBlock(
      UpdaterGoalDescription description) {
    return description.parameters().stream()
        .map(step -> copyFromProjection(step, description))
        .collect(ZeroUtil.joinCodeBlocks);
  }

  private static CodeBlock copyFromProjection(
      ProjectedParameter step,
      UpdaterGoalDescription description) {
    return switch (step.projectionInfo()) {
      case GetterMethod getterMethod -> copyFromMethod(description, getterMethod, step);
      case FieldAccess fieldAccess -> copyFromField(description, fieldAccess);
    };
  }

  private static CodeBlock copyFromField(
      UpdaterGoalDescription description,
      FieldAccess projection) {
    String field = projection.fieldName;
    ParameterSpec parameter = toBuilderParameter(description);
    ParameterSpec updater = varUpdater(description);
    CodeBlock.Builder builder = CodeBlock.builder();
    return builder.addStatement("$N.$N = $N.$N",
        updater, field, parameter, field).build();
  }

  private static CodeBlock copyFromMethod(
      UpdaterGoalDescription description,
      GetterMethod projection,
      ProjectedParameter step) {
    ParameterSpec parameter = toBuilderParameter(description);
    ParameterSpec updater = varUpdater(description);
    String field = step.name();
    CodeBlock.Builder builder = CodeBlock.builder();
    return builder.addStatement("$N.$N = $N.$N()",
        updater, field, parameter, projection.methodName).build();
  }

  static ParameterSpec toBuilderParameter(
      UpdaterGoalDescription description) {
    GoalDetails details = description.details();
    TypeName goalType = details.goalType();
    return parameterSpec(goalType, downcase(simpleName(goalType)));
  }

  static CodeBlock initVarUpdater(ParameterSpec varUpdater) {
    return statement("$T $N = new $T()", varUpdater.type(), varUpdater, varUpdater.type());
  }

  static ParameterSpec varUpdater(UpdaterGoalDescription description) {
    TypeName updaterType = implType(description);
    return parameterSpec(updaterType, "_updater");
  }

  static Set<TypeName> thrownByProjections(UpdaterGoalDescription description) {
    return description.parameters().stream()
        .map(ProjectedParameter::projectionInfo)
        .map(DtoProjectionInfo::thrownTypes)
        .flatMap(List::stream)
        .collect(toSet());
  }

  private UpdaterMethod() {
    throw new UnsupportedOperationException("no instances");
  }
}
