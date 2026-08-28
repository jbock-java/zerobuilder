package net.zerobuilder.modules.updater;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import io.jbock.simple.Inject;
import net.zerobuilder.compiler.generate.*;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.FieldAccess;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.GetterMethod;

import java.util.List;
import java.util.Set;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static java.util.stream.Collectors.toSet;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.*;

final class UpdaterMethod {
  private final GoalDescription description;
  private final Updater updater;

  @Inject
  UpdaterMethod(
      GoalDescription description,
      Updater updater) {
    this.description = description;
    this.updater = updater;
  }

  MethodSpec updaterMethod() {
    ParameterSpec updater = varUpdater();
    return methodBuilder("builder")
        .addExceptions(thrownByProjections())
        .addParameter(toBuilderParameter())
        .addTypeVariables(description.details().instanceTypeParameters())
        .returns(updater.type())
        .addCode(initVarUpdater(updater))
        .addCode(copyBlock())
        .addStatement("return $N", updater)
        .addModifiers(description.details().getAccess(STATIC))
        .build();
  }

  private CodeBlock copyBlock() {
    return description.parameters().stream()
        .map(this::copyFromProjection)
        .collect(ZeroUtil.joinCodeBlocks());
  }

  private CodeBlock copyFromProjection(ProjectedParameter step) {
    return switch (step.projectionInfo()) {
      case GetterMethod getterMethod -> copyFromMethod(getterMethod, step);
      case FieldAccess fieldAccess -> copyFromField(fieldAccess);
    };
  }

  private CodeBlock copyFromField(FieldAccess projection) {
    String field = projection.fieldName();
    ParameterSpec parameter = toBuilderParameter();
    ParameterSpec updater = varUpdater();
    CodeBlock.Builder builder = CodeBlock.builder();
    return builder.addStatement("$N.$N = $N.$N",
        updater, field, parameter, field).build();
  }

  private CodeBlock copyFromMethod(
      GetterMethod projection,
      ProjectedParameter step) {
    ParameterSpec parameter = toBuilderParameter();
    ParameterSpec updater = varUpdater();
    String field = step.name();
    CodeBlock.Builder builder = CodeBlock.builder();
    return builder.addStatement("$N.$N = $N.$N()",
        updater, field, parameter, projection.methodName()).build();
  }

  ParameterSpec toBuilderParameter() {
    GoalDetails details = description.details();
    TypeName goalType = details.goalType();
    return parameterSpec(goalType, downcase(simpleName(goalType)));
  }

  static CodeBlock initVarUpdater(ParameterSpec varUpdater) {
    return statement("$T $N = new $T()", varUpdater.type(), varUpdater, varUpdater.type());
  }

  ParameterSpec varUpdater() {
    return parameterSpec(updater.implType(), "updater");
  }

  Set<TypeName> thrownByProjections() {
    return description.parameters().stream()
        .map(ProjectedParameter::projectionInfo)
        .map(DtoProjectionInfo::thrownTypes)
        .flatMap(List::stream)
        .collect(toSet());
  }
}
