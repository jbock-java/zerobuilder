package net.zerobuilder.modules.updater;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import java.util.List;
import java.util.Set;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoProjectionInfo;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.FieldAccess;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.GetterMethod;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.ZeroUtil;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static java.util.stream.Collectors.toSet;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.modules.updater.InstanceWorld.instanceGoalMethod;
import static net.zerobuilder.modules.updater.RegularUpdater.implType;

final class Generator {

  static List<TypeVariableName> instanceTypeParameters(AbstractRegularDetails details) {
    return switch (details) {
      case DtoGoalDetails.ConstructorGoalDetails constructor -> constructor.instanceTypeParameters;
      case DtoGoalDetails.StaticMethodGoalDetails staticMethod -> List.of();
      case DtoGoalDetails.InstanceMethodGoalDetails instanceMethod -> instanceMethod.instanceTypeParameters;
    };
  }

  private static BuilderMethod normalGoalMethod(AbstractRegularDetails details, ProjectedRegularGoalDescription description) {
    ParameterSpec updater = varUpdater(description);
    MethodSpec method = methodBuilder(RegularUpdater.methodName(description))
        .addExceptions(thrownByProjections(description))
        .addParameter(toBuilderParameter(description))
        .addTypeVariables(instanceTypeParameters(description.details))
        .returns(updater.type())
        .addCode(initVarUpdater(description, updater))
        .addCode(copyBlock(description))
        .addStatement("return $N", updater)
        .addModifiers(details.access(STATIC))
        .build();
    return new BuilderMethod(details.name(), method);
  }

  static BuilderMethod goalMethod(AbstractRegularDetails details, ProjectedRegularGoalDescription description) {
    return switch (details) {
      case DtoGoalDetails.ConstructorGoalDetails constructor -> normalGoalMethod(constructor, description);
      case DtoGoalDetails.StaticMethodGoalDetails staticMethod -> normalGoalMethod(staticMethod, description);
      case DtoGoalDetails.InstanceMethodGoalDetails instanceMethod -> instanceGoalMethod(instanceMethod, description);
    };
  }

  static CodeBlock copyBlock(ProjectedRegularGoalDescription description) {
    return description.parameters.stream()
        .map(step -> copyFromProjection(step, description))
        .collect(ZeroUtil.joinCodeBlocks);
  }

  private static CodeBlock copyFromProjection(ProjectedParameter step, ProjectedRegularGoalDescription description) {
    return switch (step.projectionInfo) {
      case GetterMethod getterMethod -> copyFromMethod(description, getterMethod, step);
      case FieldAccess fieldAccess -> copyFromField(description, fieldAccess);
    };
  }

  private static CodeBlock copyFromField(ProjectedRegularGoalDescription description,
                                         FieldAccess projection) {
    String field = projection.fieldName;
    ParameterSpec parameter = toBuilderParameter(description);
    ParameterSpec updater = varUpdater(description);
    CodeBlock.Builder builder = CodeBlock.builder();
    return builder.addStatement("$N.$N = $N.$N",
        updater, field, parameter, field).build();
  }

  private static CodeBlock copyFromMethod(ProjectedRegularGoalDescription description,
                                          GetterMethod projection, ProjectedParameter step) {
    ParameterSpec parameter = toBuilderParameter(description);
    ParameterSpec updater = varUpdater(description);
    String field = step.name;
    CodeBlock.Builder builder = CodeBlock.builder();
    return builder.addStatement("$N.$N = $N.$N()",
        updater, field, parameter, projection.methodName).build();
  }

  static ParameterSpec toBuilderParameter(ProjectedRegularGoalDescription description) {
    AbstractRegularDetails details = description.details;
    TypeName goalType = details.type();
    return parameterSpec(goalType, downcase(simpleName(goalType)));
  }

  static CodeBlock initVarUpdater(ProjectedRegularGoalDescription description, ParameterSpec varUpdater) {
    return statement("$T $N = new $T()", varUpdater.type(), varUpdater, varUpdater.type());
  }

  static ParameterSpec varUpdater(ProjectedRegularGoalDescription description) {
    TypeName updaterType = implType(description);
    return parameterSpec(updaterType, "_updater");
  }

  static Set<TypeName> thrownByProjections(ProjectedRegularGoalDescription description) {
    return description.parameters.stream()
        .map(parameter -> parameter.projectionInfo)
        .map(DtoProjectionInfo::thrownTypes)
        .flatMap(List::stream)
        .collect(toSet());
  }

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
