package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBuildersContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalDetails;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.FieldAccess;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfoCases;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionMethod;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.NullPolicy.ALLOW;
import static net.zerobuilder.compiler.generate.DtoBuildersContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builderImplType;
import static net.zerobuilder.compiler.generate.DtoProjectionInfo.thrownTypes;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.goalDetails;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.isInstance;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.regularSteps;
import static net.zerobuilder.compiler.generate.Generator.stepsField;
import static net.zerobuilder.compiler.generate.Generator.updaterField;
import static net.zerobuilder.compiler.generate.Updater.updaterType;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.statement;

final class GeneratorV {

  static final Function<RegularGoalContext, BuilderMethod> goalToToBuilder =
      goal -> {
        RegularGoalDetails details = goalDetails.apply(goal);
        ParameterSpec updater = varUpdater(goal);
        MethodSpec method = methodBuilder(details.name + "ToBuilder")
            .addExceptions(thrownByProjections(goal))
            .addParameter(toBuilderParameter(goal))
            .returns(updater.type)
            .addCode(nullCheckingBlock(goal))
            .addCode(initUpdater(goal, updater))
            .addCode(copyBlock(goal))
            .addStatement("return $N", updater)
            .addModifiers(details.goalOptions.toBuilderAccess.modifiers(STATIC))
            .build();
        return new BuilderMethod(details.name, method);
      };

  private static CodeBlock copyBlock(RegularGoalContext goal) {
    ProjectionInfoCases<CodeBlock, RegularStep> copy = copyField(goal);
    return regularSteps.apply(goal).stream()
        .map(step -> step.validParameter.projectionInfo.accept(copy, step))
        .collect(Utilities.join);
  }

  private static CodeBlock nullCheckingBlock(RegularGoalContext goal) {
    ProjectionInfoCases<CodeBlock, RegularStep> nullChecks = nullChecks(goal);
    CodeBlock.Builder builder = CodeBlock.builder();
    for (RegularStep step : regularSteps.apply(goal)) {
      ProjectionInfo projectionInfo = step.validParameter.projectionInfo;
      builder.add(projectionInfo.accept(nullChecks, step));
    }
    return builder.build();
  }

  private static ProjectionInfoCases<CodeBlock, RegularStep> copyField(RegularGoalContext goal) {
    return new ProjectionInfoCases<CodeBlock, RegularStep>() {
      @Override
      public CodeBlock projectionMethod(ProjectionMethod projection, RegularStep step) {
        ParameterSpec parameter = toBuilderParameter(goal);
        ParameterSpec updater = varUpdater(goal);
        String field = step.validParameter.name;
        return statement("$N.$N = $N.$N()",
            updater, field, parameter, projection.methodName);
      }
      @Override
      public CodeBlock fieldAccess(FieldAccess projection, RegularStep step) {
        String field = projection.fieldName;
        ParameterSpec parameter = toBuilderParameter(goal);
        ParameterSpec updater = varUpdater(goal);
        return statement("$N.$N = $N.$N",
            updater, field, parameter, field);
      }
      @Override
      public CodeBlock none() {
        throw new IllegalStateException("should never happen");
      }
    };
  }

  private static ProjectionInfoCases<CodeBlock, RegularStep> nullChecks(RegularGoalContext goal) {
    return new ProjectionInfoCases<CodeBlock, RegularStep>() {
      @Override
      public CodeBlock projectionMethod(ProjectionMethod projection, RegularStep step) {
        if (step.validParameter.nullPolicy == ALLOW) {
          return emptyCodeBlock;
        }
        ParameterSpec parameter = toBuilderParameter(goal);
        String name = step.validParameter.name;
        return CodeBlock.builder()
            .beginControlFlow("if ($N.$N() == null)", parameter, projection.methodName)
            .addStatement("throw new $T($S)", NullPointerException.class, name)
            .endControlFlow().build();
      }
      @Override
      public CodeBlock fieldAccess(FieldAccess projection, RegularStep step) {
        if (step.validParameter.nullPolicy == ALLOW) {
          return emptyCodeBlock;
        }
        ParameterSpec parameter = toBuilderParameter(goal);
        String name = step.validParameter.name;
        return CodeBlock.builder()
            .beginControlFlow("if ($N.$N == null)", parameter, name)
            .addStatement("throw new $T($S)", NullPointerException.class, name)
            .endControlFlow().build();
      }
      @Override
      public CodeBlock none() {
        throw new IllegalStateException("should never happen");
      }
    };
  }
  private static ParameterSpec toBuilderParameter(RegularGoalContext goal) {
    RegularGoalDetails details = goalDetails.apply(goal);
    TypeName goalType = details.goalType;
    return parameterSpec(goalType, downcase(((ClassName) goalType.box()).simpleName()));
  }

  private static CodeBlock initUpdater(RegularGoalContext goal, ParameterSpec updater) {
    BuildersContext buildersContext = DtoRegularGoalContext.buildersContext.apply(goal);
    if (buildersContext.lifecycle == REUSE_INSTANCES) {
      FieldSpec cache = buildersContext.cache;
      FieldSpec updaterField = updaterField(goal);
      return statement("$T $N = $N.get().$N",
          updater.type, updater, cache, updaterField);
    } else {
      return statement("$T $N = new $T()",
          updater.type, updater, updater.type);
    }
  }

  private static ParameterSpec varUpdater(RegularGoalContext goal) {
    ClassName updaterType = updaterType(goal);
    return parameterSpec(updaterType, "updater");
  }


  static final Function<RegularGoalContext, BuilderMethod> goalToBuilder
      = goal -> {
    RegularGoalDetails regularGoalDetails = goalDetails.apply(goal);
    List<RegularStep> steps = regularSteps.apply(goal);
    String name = DtoGoalContext.goalName.apply(goal);
    MethodSpec.Builder method = methodBuilder(name + "Builder")
        .returns(steps.get(0).thisType)
        .addModifiers(regularGoalDetails.goalOptions.builderAccess.modifiers(STATIC));
    ParameterSpec builder = builderInstance(goal);
    method.addCode(initBuilder(goal, builder));
    if (isInstance.test(goal)) {
      BuildersContext buildersContext = DtoRegularGoalContext.buildersContext.apply(goal);
      ParameterSpec parameter = parameterSpec(buildersContext.type,
          downcase(buildersContext.type.simpleName()));
      method.addParameter(parameter)
          .addStatement("$N.$N = $N", builder, buildersContext.field, parameter);
    }
    MethodSpec methodSpec = method.addStatement("return $N", builder).build();
    return new BuilderMethod(name, methodSpec);
  };

  private static CodeBlock initBuilder(RegularGoalContext goal, ParameterSpec builder) {
    BuildersContext buildersContext = DtoRegularGoalContext.buildersContext.apply(goal);
    return buildersContext.lifecycle.recycle()
        ? statement("$T $N = $N.get().$N", builder.type, builder, buildersContext.cache, stepsField(goal))
        : statement("$T $N = new $T()", builder.type, builder, builder.type);
  }

  private static ParameterSpec builderInstance(RegularGoalContext goal) {
    ClassName stepsType = builderImplType(goal);
    return parameterSpec(stepsType, downcase(stepsType.simpleName()));
  }

  private static Set<TypeName> thrownByProjections(RegularGoalContext goal) {
    return regularSteps.apply(goal).stream()
        .map(step -> step.validParameter)
        .map(parameter -> parameter.projectionInfo)
        .map(thrownTypes)
        .map(List::stream)
        .flatMap(Function.identity())
        .collect(Collectors.toSet());
  }

  private GeneratorV() {
    throw new UnsupportedOperationException("no instances");
  }
}
