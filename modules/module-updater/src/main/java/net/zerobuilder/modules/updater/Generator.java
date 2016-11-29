package net.zerobuilder.modules.updater;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.FieldAccess;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfoCases;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionMethod;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.ZeroUtil;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.NullPolicy.ALLOW;
import static net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.projectedRegularGoalContextCases;
import static net.zerobuilder.compiler.generate.DtoProjectionInfo.projectionInfoCases;
import static net.zerobuilder.compiler.generate.DtoProjectionInfo.thrownTypes;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.modules.updater.RegularUpdater.cacheField;
import static net.zerobuilder.modules.updater.RegularUpdater.implType;

final class Generator {

  private static final Function<ProjectedRegularGoalContext, BuilderMethod> normalGoalMethod =
      goal -> {
        AbstractRegularDetails details = goal.details();
        ParameterSpec updater = varUpdater(goal);
        MethodSpec method = methodBuilder(RegularUpdater.methodName(goal))
            .addExceptions(thrownByProjections(goal))
            .addParameter(toBuilderParameter(goal))
            .addTypeVariables(goal.instanceTypeParameters())
            .returns(updater.type)
            .addCode(nullCheckingBlock(goal))
            .addCode(initVarUpdater(goal, updater))
            .addCode(copyBlock(goal))
            .addStatement("return $N", updater)
            .addModifiers(details.access(STATIC))
            .build();
        return new BuilderMethod(details.name, method);
      };

  static final Function<ProjectedRegularGoalContext, BuilderMethod> goalMethod =
      projectedRegularGoalContextCases(
          normalGoalMethod,
          InstanceWorld::instanceGoalMethod,
          normalGoalMethod);

  static CodeBlock copyBlock(ProjectedRegularGoalContext goal) {
    return goal.description().parameters().stream()
        .map(copyField(goal))
        .collect(ZeroUtil.joinCodeBlocks);
  }

  static CodeBlock nullCheckingBlock(ProjectedRegularGoalContext goal) {
    ProjectionInfoCases<CodeBlock, ProjectedParameter> nullChecks = nullChecks(goal);
    CodeBlock.Builder builder = CodeBlock.builder();
    for (ProjectedParameter step : goal.description().parameters()) {
      builder.add(step.projectionInfo.accept(nullChecks, step));
    }
    return builder.build();
  }

  private static Function<ProjectedParameter, CodeBlock> copyField(ProjectedRegularGoalContext goal) {
    BiFunction<ProjectionInfo, ProjectedParameter, CodeBlock> copy =
        projectionInfoCases(
            copyFromMethod(goal),
            copyFromField(goal));
    return step -> copy.apply(step.projectionInfo, step);
  }

  private static BiFunction<FieldAccess, ProjectedParameter, CodeBlock> copyFromField(ProjectedRegularGoalContext goal) {
    return (FieldAccess projection, ProjectedParameter step) -> {
      String field = projection.fieldName;
      ParameterSpec parameter = toBuilderParameter(goal);
      ParameterSpec updater = varUpdater(goal);
      return statement("$N.$N = $N.$N",
          updater, field, parameter, field);
    };
  }

  private static BiFunction<ProjectionMethod, ProjectedParameter, CodeBlock> copyFromMethod(ProjectedRegularGoalContext goal) {
    return (ProjectionMethod projection, ProjectedParameter step) -> {
      ParameterSpec parameter = toBuilderParameter(goal);
      ParameterSpec updater = varUpdater(goal);
      String field = step.name;
      return statement("$N.$N = $N.$N()",
          updater, field, parameter, projection.methodName);
    };
  }

  private static ProjectionInfoCases<CodeBlock, ProjectedParameter> nullChecks(ProjectedRegularGoalContext goal) {
    return new ProjectionInfoCases<CodeBlock, ProjectedParameter>() {
      @Override
      public CodeBlock projectionMethod(ProjectionMethod projection, ProjectedParameter step) {
        if (step.nullPolicy == ALLOW) {
          return emptyCodeBlock;
        }
        ParameterSpec parameter = toBuilderParameter(goal);
        String name = step.name;
        return CodeBlock.builder()
            .beginControlFlow("if ($N.$N() == null)", parameter, projection.methodName)
            .addStatement("throw new $T($S)", NullPointerException.class, name)
            .endControlFlow().build();
      }
      @Override
      public CodeBlock fieldAccess(FieldAccess projection, ProjectedParameter step) {
        if (step.nullPolicy == ALLOW) {
          return emptyCodeBlock;
        }
        ParameterSpec parameter = toBuilderParameter(goal);
        String name = step.name;
        return CodeBlock.builder()
            .beginControlFlow("if ($N.$N == null)", parameter, name)
            .addStatement("throw new $T($S)", NullPointerException.class, name)
            .endControlFlow().build();
      }
    };
  }

  static ParameterSpec toBuilderParameter(ProjectedRegularGoalContext goal) {
    AbstractRegularDetails details = goal.details();
    TypeName goalType = details.type();
    return parameterSpec(goalType, downcase(simpleName(goalType)));
  }

  static CodeBlock initVarUpdater(ProjectedRegularGoalContext goal, ParameterSpec varUpdater) {
    Optional<FieldSpec> udpaterField = cacheField(goal);
    if (udpaterField.isPresent()) {
      GoalContext context = goal.context();
      ParameterSpec varContext = parameterSpec(context.generatedType, "context");
      FieldSpec cache = context.cache.get();
      return CodeBlock.builder()
          .addStatement("$T $N = $N.get()", varContext.type, varContext, cache)
          .beginControlFlow("if ($N.$N._currently_in_use)", varContext, udpaterField.get())
          .addStatement("$N.$N = new $T()", varContext, udpaterField.get(), varUpdater.type)
          .endControlFlow()
          .addStatement("$T $N = $N.$N", varUpdater.type, varUpdater, varContext, udpaterField.get())
          .addStatement("$N._currently_in_use = true", varUpdater)
          .build();
    } else {
      return statement("$T $N = new $T()", varUpdater.type, varUpdater, varUpdater.type);
    }
  }

  static ParameterSpec varUpdater(ProjectedRegularGoalContext goal) {
    TypeName updaterType = implType(goal);
    return parameterSpec(updaterType, "updater");
  }

  static Set<TypeName> thrownByProjections(ProjectedRegularGoalContext goal) {
    return goal.description().parameters().stream()
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
