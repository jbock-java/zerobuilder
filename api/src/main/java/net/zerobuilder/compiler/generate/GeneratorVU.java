package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractRegularGoalDetails;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.FieldAccess;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfoCases;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionMethod;
import net.zerobuilder.compiler.generate.DtoRegularGoal.AbstractRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularParameter.AbstractRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;

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
import static net.zerobuilder.compiler.generate.DtoProjectionInfo.projectionInfoCases;
import static net.zerobuilder.compiler.generate.DtoProjectionInfo.thrownTypes;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.statement;

final class GeneratorVU {

  static final Function<AbstractRegularGoalContext, BuilderMethod> goalToUpdaterV =
      goal -> {
        AbstractRegularGoalDetails details = goal.regularDetails();
        ParameterSpec updater = varUpdater(goal);
        MethodSpec method = methodBuilder(goal.methodName())
            .addExceptions(thrownByProjections(goal))
            .addParameter(toBuilderParameter(goal))
            .returns(updater.type)
            .addCode(nullCheckingBlock(goal))
            .addCode(initUpdater(goal, updater))
            .addCode(copyBlock(goal))
            .addStatement("return $N", updater)
            .addModifiers(details.option.access.modifiers(STATIC))
            .build();
        return new BuilderMethod(details.name, method);
      };

  private static CodeBlock copyBlock(AbstractRegularGoalContext goal) {
    return goal.regularSteps().stream()
        .map(copyField(goal))
        .collect(Utilities.joinCodeBlocks);
  }

  private static CodeBlock nullCheckingBlock(AbstractRegularGoalContext goal) {
    ProjectionInfoCases<CodeBlock, AbstractRegularStep> nullChecks = nullChecks(goal);
    CodeBlock.Builder builder = CodeBlock.builder();
    for (AbstractRegularStep step : goal.regularSteps()) {
      Optional<ProjectionInfo> projectionInfo = step.regularParameter().projectionInfo();
      builder.add(projectionInfo.get().accept(nullChecks, step));
    }
    return builder.build();
  }

  private static Function<AbstractRegularStep, CodeBlock> copyField(AbstractRegularGoalContext goal) {
    BiFunction<ProjectionInfo, AbstractRegularStep, CodeBlock> copy = projectionInfoCases(
        copyFromMethod(goal),
        copyFromField(goal));
    return step -> copy.apply(step.regularParameter().projectionInfo().get(), step);
  }

  private static BiFunction<FieldAccess, AbstractRegularStep, CodeBlock> copyFromField(AbstractRegularGoalContext goal) {
    return (FieldAccess projection, AbstractRegularStep step) -> {
      String field = projection.fieldName;
      ParameterSpec parameter = toBuilderParameter(goal);
      ParameterSpec updater = varUpdater(goal);
      return statement("$N.$N = $N.$N",
          updater, field, parameter, field);
    };
  }

  private static BiFunction<ProjectionMethod, AbstractRegularStep, CodeBlock> copyFromMethod(AbstractRegularGoalContext goal) {
    return (ProjectionMethod projection, AbstractRegularStep step) -> {
      ParameterSpec parameter = toBuilderParameter(goal);
      ParameterSpec updater = varUpdater(goal);
      String field = step.regularParameter().name;
      return statement("$N.$N = $N.$N()",
          updater, field, parameter, projection.methodName);
    };
  }

  private static ProjectionInfoCases<CodeBlock, AbstractRegularStep> nullChecks(AbstractRegularGoalContext goal) {
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

  private static ParameterSpec toBuilderParameter(AbstractRegularGoalContext goal) {
    AbstractRegularGoalDetails details = goal.regularDetails();
    TypeName goalType = details.goalType;
    return parameterSpec(goalType, downcase(((ClassName) goalType.box()).simpleName()));
  }

  private static CodeBlock initUpdater(AbstractRegularGoalContext goal, ParameterSpec updater) {
    BuildersContext context = goal.context();
    if (context.lifecycle == REUSE_INSTANCES) {
      FieldSpec cache = context.cache.get();
      FieldSpec updaterField = goal.cacheField();
      return statement("$T $N = $N.get().$N",
          updater.type, updater, cache, updaterField);
    } else {
      return statement("$T $N = new $T()",
          updater.type, updater, updater.type);
    }
  }

  private static ParameterSpec varUpdater(AbstractRegularGoalContext goal) {
    ClassName updaterType = goal.implType();
    return parameterSpec(updaterType, "updater");
  }

  private static Set<TypeName> thrownByProjections(AbstractRegularGoalContext goal) {
    return goal.regularSteps().stream()
        .map(step -> step.regularParameter())
        .map(AbstractRegularParameter::projectionInfo)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(thrownTypes)
        .map(List::stream)
        .flatMap(identity())
        .collect(toSet());
  }

  private GeneratorVU() {
    throw new UnsupportedOperationException("no instances");
  }
}
