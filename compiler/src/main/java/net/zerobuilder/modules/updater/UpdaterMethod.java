package net.zerobuilder.modules.updater;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import io.jbock.simple.Inject;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.FieldAccess;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.GetterMethod;
import net.zerobuilder.compiler.generate.GoalDescription;
import net.zerobuilder.compiler.generate.GoalDetails;
import net.zerobuilder.compiler.generate.ProjectedParameter;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;

record UpdaterMethod(
    GoalDescription description,
    Updater updater) {

  @Inject
  UpdaterMethod {
  }

  MethodSpec nullaryMethod() {
    return methodBuilder("builder")
        .addTypeVariables(description.details().instanceTypeParameters())
        .returns(updater.implType())
        .addStatement("return new $T()", updater.implType())
        .addModifiers(description.details().getAccess(STATIC))
        .build();
  }

  MethodSpec unaryMethod() {
    ParameterSpec varUpdater = varUpdater();
    return methodBuilder("builder")
        .addParameter(toBuilderParameter())
        .addTypeVariables(description.details().instanceTypeParameters())
        .returns(updater.implType())
        .addCode(initVarUpdater(varUpdater))
        .addCode(copyBlock())
        .addStatement("return $N", varUpdater)
        .addModifiers(description.details().getAccess(STATIC))
        .build();
  }

  private CodeBlock copyBlock() {
    return description.parameters().stream()
        .map(this::copyFromProjection)
        .collect(CodeBlock.joining(""));
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
    String field = step.stepName();
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
}
