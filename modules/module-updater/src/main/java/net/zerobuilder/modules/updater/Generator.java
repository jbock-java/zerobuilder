package net.zerobuilder.modules.updater;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.FieldAccess;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfoCases;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionMethod;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.ZeroUtil;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoGoalDetails.regularDetailsCases;
import static net.zerobuilder.compiler.generate.DtoProjectionInfo.projectionInfoCases;
import static net.zerobuilder.compiler.generate.DtoProjectionInfo.thrownTypes;
import static net.zerobuilder.compiler.generate.NullPolicy.ALLOW;
import static net.zerobuilder.compiler.generate.NullPolicy.REJECT;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.modules.updater.RegularUpdater.implType;
import static net.zerobuilder.modules.updater.RegularUpdater.isReusable;

final class Generator {

  private static final Function<AbstractRegularDetails, List<TypeVariableName>> instanceTypeParameters =
      regularDetailsCases(
          constructor -> constructor.instanceTypeParameters,
          staticMethod -> emptyList(),
          instanceMethod -> instanceMethod.instanceTypeParameters);

  private static final BiFunction<AbstractRegularDetails, ProjectedRegularGoalDescription, BuilderMethod> normalGoalMethod =
      (details, description) -> {
        ParameterSpec updater = varUpdater(description);
        MethodSpec method = methodBuilder(RegularUpdater.methodName(description))
            .addExceptions(thrownByProjections(description))
            .addParameter(toBuilderParameter(description))
            .addTypeVariables(instanceTypeParameters.apply(description.details))
            .returns(updater.type)
            .addCode(nullCheckingBlock(description))
            .addCode(initVarUpdater(description, updater))
            .addCode(copyBlock(description))
            .addStatement("return $N", updater)
            .addModifiers(details.access(STATIC))
            .build();
        return new BuilderMethod(details.name, method);
      };

  static final BiFunction<AbstractRegularDetails, ProjectedRegularGoalDescription, BuilderMethod> goalMethod =
      regularDetailsCases(
          normalGoalMethod,
          normalGoalMethod,
          InstanceWorld::instanceGoalMethod);

  static CodeBlock copyBlock(ProjectedRegularGoalDescription description) {
    return description.parameters.stream()
        .map(copyField(description))
        .collect(ZeroUtil.joinCodeBlocks);
  }

  private static CodeBlock nullCheckingBlock(ProjectedRegularGoalDescription description) {
    CodeBlock.Builder builder = CodeBlock.builder();
    for (ProjectedParameter parameter : description.parameters) {
      builder.add(parameter.projectionInfo.accept(Generator.nullChecks, description, parameter));
    }
    return builder.build();
  }

  private static Function<ProjectedParameter, CodeBlock> copyField(ProjectedRegularGoalDescription description) {
    BiFunction<ProjectionInfo, ProjectedParameter, CodeBlock> copy =
        projectionInfoCases(
            (projection, step) -> copyFromMethod(description, projection, step),
            (projection, step) -> copyFromField(description, projection, step));
    return step -> copy.apply(step.projectionInfo, step);
  }

  private static CodeBlock copyFromField(ProjectedRegularGoalDescription description,
                                         FieldAccess projection, ProjectedParameter step) {
    String field = projection.fieldName;
    ParameterSpec parameter = toBuilderParameter(description);
    ParameterSpec updater = varUpdater(description);
    CodeBlock.Builder builder = CodeBlock.builder();
    if (step.nullPolicy == REJECT) {
      builder.add(CodeBlock.builder()
          .beginControlFlow("if ($N.$N == null)", parameter, field)
          .addStatement("throw new $T($S)",
              NullPointerException.class, "projection: " + field)
          .endControlFlow().build());
    }
    return builder.addStatement("$N.$N = $N.$N",
        updater, field, parameter, field).build();
  }

  private static CodeBlock copyFromMethod(ProjectedRegularGoalDescription description,
                                          ProjectionMethod projection, ProjectedParameter step) {
    ParameterSpec parameter = toBuilderParameter(description);
    ParameterSpec updater = varUpdater(description);
    String field = step.name;
    CodeBlock.Builder builder = CodeBlock.builder();
    if (step.nullPolicy == REJECT) {
      builder.add(CodeBlock.builder()
          .beginControlFlow("if ($N.$N() == null)", parameter, projection.methodName)
          .addStatement("throw new $T($S)",
              NullPointerException.class, "projection: " + projection.methodName)
          .endControlFlow().build());
    }
    return builder.addStatement("$N.$N = $N.$N()",
        updater, field, parameter, projection.methodName).build();
  }

  private static final ProjectionInfoCases<CodeBlock, ProjectedRegularGoalDescription, ProjectedParameter> nullChecks =
      new ProjectionInfoCases<CodeBlock, ProjectedRegularGoalDescription, ProjectedParameter>() {
        @Override
        public CodeBlock projectionMethod(ProjectionMethod projection,
                                          ProjectedRegularGoalDescription description,
                                          ProjectedParameter step) {
          if (step.nullPolicy == ALLOW) {
            return emptyCodeBlock;
          }
          ParameterSpec parameter = toBuilderParameter(description);
          String name = step.name;
          return CodeBlock.builder()
              .beginControlFlow("if ($N.$N() == null)", parameter, projection.methodName)
              .addStatement("throw new $T($S)", NullPointerException.class, name)
              .endControlFlow().build();
        }
        @Override
        public CodeBlock fieldAccess(FieldAccess projection,
                                     ProjectedRegularGoalDescription description,
                                     ProjectedParameter step) {
          if (step.nullPolicy == ALLOW) {
            return emptyCodeBlock;
          }
          ParameterSpec parameter = toBuilderParameter(description);
          String name = step.name;
          return CodeBlock.builder()
              .beginControlFlow("if ($N.$N == null)", parameter, name)
              .addStatement("throw new $T($S)", NullPointerException.class, name)
              .endControlFlow().build();
        }
      };


  static ParameterSpec toBuilderParameter(ProjectedRegularGoalDescription description) {
    AbstractRegularDetails details = description.details;
    TypeName goalType = details.type();
    return parameterSpec(goalType, downcase(simpleName(goalType)));
  }

  static CodeBlock initVarUpdater(ProjectedRegularGoalDescription description, ParameterSpec varUpdater) {
    if (isReusable.apply(description.details)) {
      GoalContext context = description.context;
      FieldSpec cache = context.cache(rawClassName(varUpdater.type));
      return CodeBlock.builder()
          .addStatement("$T $N = $N.get()", varUpdater.type, varUpdater, cache)
          .beginControlFlow("if ($N._currently_in_use)", varUpdater)
          .addStatement("$N.remove()", cache)
          .addStatement("$N = $N.get()", varUpdater, cache)
          .endControlFlow()
          .addStatement("$N._currently_in_use = true", varUpdater)
          .build();
    } else {
      return statement("$T $N = new $T()", varUpdater.type, varUpdater, varUpdater.type);
    }
  }

  static ParameterSpec varUpdater(ProjectedRegularGoalDescription description) {
    TypeName updaterType = implType(description);
    return parameterSpec(updaterType, "_updater");
  }

  static Set<TypeName> thrownByProjections(ProjectedRegularGoalDescription description) {
    return description.parameters.stream()
        .map(parameter -> parameter.projectionInfo)
        .map(thrownTypes)
        .map(List::stream)
        .flatMap(identity())
        .collect(toSet());
  }

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
