package net.zerobuilder.modules.updater;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.FieldAccess;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfoCases;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionMethod;
import net.zerobuilder.compiler.generate.DtoRegularParameter.AbstractRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;
import net.zerobuilder.compiler.generate.DtoRegularStep.ProjectedRegularStep;
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
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.goalDetails;
import static net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.steps;
import static net.zerobuilder.compiler.generate.DtoProjectionInfo.projectionInfoCases;
import static net.zerobuilder.compiler.generate.DtoProjectionInfo.thrownTypes;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;

final class GeneratorVU {

  private final Updater updater;

  GeneratorVU(Updater updater) {
    this.updater = updater;
  }

  BuilderMethod goalToUpdaterV(ProjectedRegularGoalContext goal) {
    AbstractRegularDetails details = goalDetails.apply(goal);
    ParameterSpec updater = varUpdater(goal);
    MethodSpec method = methodBuilder(this.updater.legacyMethodName(goal))
        .addExceptions(thrownByProjections(goal))
        .addParameter(toBuilderParameter(goal))
        .returns(updater.type)
        .addCode(nullCheckingBlock(goal))
        .addCode(initVarUpdater(goal, updater))
        .addCode(copyBlock(goal))
        .addStatement("return $N", updater)
        .addModifiers(details.access(STATIC))
        .build();
    return new BuilderMethod(details.name, method);
  }

  private CodeBlock copyBlock(ProjectedRegularGoalContext goal) {
    return steps.apply(goal).stream()
        .map(copyField(goal))
        .collect(ZeroUtil.joinCodeBlocks);
  }

  private CodeBlock nullCheckingBlock(ProjectedRegularGoalContext goal) {
    ProjectionInfoCases<CodeBlock, AbstractRegularStep> nullChecks = nullChecks(goal);
    CodeBlock.Builder builder = CodeBlock.builder();
    for (ProjectedRegularStep step : steps.apply(goal)) {
      Optional<ProjectionInfo> projectionInfo = step.regularParameter().projectionInfo();
      builder.add(projectionInfo.get().accept(nullChecks, step));
    }
    return builder.build();
  }

  private Function<AbstractRegularStep, CodeBlock> copyField(ProjectedRegularGoalContext goal) {
    BiFunction<ProjectionInfo, AbstractRegularStep, CodeBlock> copy =
        projectionInfoCases(
            copyFromMethod(goal),
            copyFromField(goal));
    return step -> copy.apply(step.regularParameter().projectionInfo().get(), step);
  }

  private BiFunction<FieldAccess, AbstractRegularStep, CodeBlock> copyFromField(ProjectedRegularGoalContext goal) {
    return (FieldAccess projection, AbstractRegularStep step) -> {
      String field = projection.fieldName;
      ParameterSpec parameter = toBuilderParameter(goal);
      ParameterSpec updater = varUpdater(goal);
      return statement("$N.$N = $N.$N",
          updater, field, parameter, field);
    };
  }

  private BiFunction<ProjectionMethod, AbstractRegularStep, CodeBlock> copyFromMethod(ProjectedRegularGoalContext goal) {
    return (ProjectionMethod projection, AbstractRegularStep step) -> {
      ParameterSpec parameter = toBuilderParameter(goal);
      ParameterSpec updater = varUpdater(goal);
      String field = step.regularParameter().name;
      return statement("$N.$N = $N.$N()",
          updater, field, parameter, projection.methodName);
    };
  }

  private ProjectionInfoCases<CodeBlock, AbstractRegularStep> nullChecks(ProjectedRegularGoalContext goal) {
    return new ProjectionInfoCases<CodeBlock, AbstractRegularStep>() {
      @Override
      public CodeBlock projectionMethod(ProjectionMethod projection, AbstractRegularStep step) {
        if (step.regularParameter().nullPolicy == ALLOW) {
          return emptyCodeBlock;
        }
        ParameterSpec parameter = toBuilderParameter(goal);
        String name = step.regularParameter().name;
        return CodeBlock.builder()
            .beginControlFlow("if ($N.$N() == null)", parameter, projection.methodName)
            .addStatement("throw new $T($S)", NullPointerException.class, name)
            .endControlFlow().build();
      }
      @Override
      public CodeBlock fieldAccess(FieldAccess projection, AbstractRegularStep step) {
        if (step.regularParameter().nullPolicy == ALLOW) {
          return emptyCodeBlock;
        }
        ParameterSpec parameter = toBuilderParameter(goal);
        String name = step.regularParameter().name;
        return CodeBlock.builder()
            .beginControlFlow("if ($N.$N == null)", parameter, name)
            .addStatement("throw new $T($S)", NullPointerException.class, name)
            .endControlFlow().build();
      }
    };
  }

  private ParameterSpec toBuilderParameter(ProjectedRegularGoalContext goal) {
    AbstractRegularDetails details = goalDetails.apply(goal);
    TypeName goalType = details.goalType;
    return parameterSpec(goalType, downcase(((ClassName) goalType.box()).simpleName()));
  }

  private CodeBlock initVarUpdater(ProjectedRegularGoalContext goal, ParameterSpec varUpdater) {
    BuildersContext context = goal.context();
    if (context.lifecycle == REUSE_INSTANCES) {
      ParameterSpec varContext = parameterSpec(context.generatedType, "context");
      FieldSpec cache = context.cache.get();
      FieldSpec updaterField = this.updater.legacyCacheField(goal);
      return CodeBlock.builder()
          .addStatement("$T $N = $N.get()", varContext.type, varContext, cache)
          .addStatement("$T $N", varUpdater.type, varUpdater)
          .beginControlFlow("if ($N.refs++ == 0)", varContext)
          .addStatement("$N = $N.$N", varUpdater, varContext, updaterField)
          .endControlFlow()
          .beginControlFlow("else")
          .addStatement("$N = new $T()", varUpdater, varUpdater.type)
          .endControlFlow()
          .build();
    } else {
      return statement("$T $N = new $T()", varUpdater.type, varUpdater, varUpdater.type);
    }
  }

  private ParameterSpec varUpdater(ProjectedRegularGoalContext goal) {
    ClassName updaterType = updater.legacyImplType(goal);
    return parameterSpec(updaterType, "updater");
  }

  private Set<TypeName> thrownByProjections(ProjectedRegularGoalContext goal) {
    return DtoProjectedRegularGoalContext.steps.apply(goal).stream()
        .map(AbstractRegularStep::regularParameter)
        .map(AbstractRegularParameter::projectionInfo)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(thrownTypes)
        .map(List::stream)
        .flatMap(identity())
        .collect(toSet());
  }
}
