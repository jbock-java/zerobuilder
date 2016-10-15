package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalDetails;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.FieldAccess;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfoCases;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionMethod;
import net.zerobuilder.compiler.generate.DtoRegularGoal.ConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.MethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.NullPolicy.ALLOW;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoGoal.GoalMethodType.INSTANCE_METHOD;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builderImplType;
import static net.zerobuilder.compiler.generate.DtoProjectionInfo.projectionInfoCases;
import static net.zerobuilder.compiler.generate.DtoProjectionInfo.thrownTypes;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.goalDetails;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.isInstance;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularGoalContextCases;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularSteps;
import static net.zerobuilder.compiler.generate.Updater.updaterType;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.statement;

final class GeneratorV {

  static final Function<RegularGoalContext, BuilderMethod> goalToUpdaterV =
      goal -> {
        RegularGoalDetails details = goalDetails.apply(goal);
        ParameterSpec updater = varUpdater(goal);
        MethodSpec method = methodBuilder(details.name + "Updater")
            .addExceptions(thrownByProjections(goal))
            .addParameter(toBuilderParameter(goal))
            .returns(updater.type)
            .addCode(nullCheckingBlock(goal))
            .addCode(initUpdater(goal, updater))
            .addCode(copyBlock(goal))
            .addStatement("return $N", updater)
            .addModifiers(details.goalOptions.access(Updater.class).modifiers(STATIC))
            .build();
        return new BuilderMethod(details.name, method);
      };

  private static CodeBlock copyBlock(RegularGoalContext goal) {
    return regularSteps.apply(goal).stream()
        .map(copyField(goal))
        .collect(Utilities.joinCodeBlocks);
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

  private static Function<RegularStep, CodeBlock> copyField(RegularGoalContext goal) {
    BiFunction<ProjectionInfo, RegularStep, CodeBlock> copy = projectionInfoCases(
        copyFromMethod(goal),
        copyFromField(goal),
        step -> {
          throw new IllegalStateException("should never happen");
        });
    return step -> copy.apply(step.validParameter.projectionInfo, step);
  }

  private static BiFunction<FieldAccess, RegularStep, CodeBlock> copyFromField(RegularGoalContext goal) {
    return (FieldAccess projection, RegularStep step) -> {
      String field = projection.fieldName;
      ParameterSpec parameter = toBuilderParameter(goal);
      ParameterSpec updater = varUpdater(goal);
      return statement("$N.$N = $N.$N",
          updater, field, parameter, field);
    };
  }

  private static BiFunction<ProjectionMethod, RegularStep, CodeBlock> copyFromMethod(RegularGoalContext goal) {
    return (ProjectionMethod projection, RegularStep step) -> {
      ParameterSpec parameter = toBuilderParameter(goal);
      ParameterSpec updater = varUpdater(goal);
      String field = step.validParameter.name;
      return statement("$N.$N = $N.$N()",
          updater, field, parameter, projection.methodName);
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
      public CodeBlock none(RegularStep step) {
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
    BuildersContext buildersContext = DtoRegularGoal.buildersContext.apply(goal);
    if (buildersContext.lifecycle == REUSE_INSTANCES) {
      FieldSpec cache = buildersContext.cache.get();
      FieldSpec updaterField = goal.updaterField();
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

  static final Function<RegularGoalContext, BuilderMethod> goalToBuilderV
      = goal -> {
    RegularGoalDetails regularGoalDetails = goalDetails.apply(goal);
    List<RegularStep> steps = regularSteps.apply(goal);
    MethodSpec.Builder method = methodBuilder(goal.name() + "Builder")
        .returns(steps.get(0).thisType)
        .addModifiers(regularGoalDetails.goalOptions.access(Builder.class).modifiers(STATIC));
    ParameterSpec builder = builderInstance(goal);
    BuildersContext context = DtoRegularGoal.buildersContext.apply(goal);
    ParameterSpec instance = parameterSpec(context.type, downcase(context.type.simpleName()));
    method.addCode(initBuilder(builder, instance).apply(goal));
    if (isInstance.test(goal)) {
      method.addParameter(instance);
    }
    MethodSpec methodSpec = method.addStatement("return $N", builder).build();
    return new BuilderMethod(goal.name(), methodSpec);
  };

  private static Function<RegularGoalContext, CodeBlock> initBuilder(
      ParameterSpec builder, ParameterSpec instance) {
    return regularGoalContextCases(
        initConstructorBuilder(builder),
        initMethodBuilder(builder, instance));
  }

  private static Function<ConstructorGoalContext, CodeBlock> initConstructorBuilder(
      ParameterSpec builder) {
    return cGoal -> {
      BuildersContext context = cGoal.context;
      TypeName type = builder.type;
      FieldSpec cache = context.cache.get();
      return context.lifecycle == REUSE_INSTANCES ?
          statement("$T $N = $N.get().$N", type, builder, cache, cGoal.builderField()) :
          statement("$T $N = new $T()", type, builder, type);
    };
  }

  private static Function<MethodGoalContext, CodeBlock> initMethodBuilder(
      ParameterSpec builder, ParameterSpec instance) {
    return mGoal -> mGoal.methodType() == INSTANCE_METHOD ?
        initInstanceMethodBuilder(mGoal, builder, instance) :
        initStaticMethodBuilder(mGoal, builder);
  }

  private static CodeBlock initInstanceMethodBuilder(
      MethodGoalContext mGoal, ParameterSpec builder, ParameterSpec instance) {
    BuildersContext context = mGoal.context;
    TypeName type = builder.type;
    FieldSpec cache = context.cache.get();
    return context.lifecycle == REUSE_INSTANCES ?
        CodeBlock.builder()
            .addStatement("$T $N = $N.get().$N", type, builder, cache, mGoal.builderField())
            .addStatement("$N.$N = $N", builder, mGoal.field(), instance)
            .build() :
        statement("$T $N = new $T($N)", type, builder, type, instance);
  }

  private static CodeBlock initStaticMethodBuilder(
      MethodGoalContext mGoal, ParameterSpec builder) {
    BuildersContext context = mGoal.context;
    TypeName type = builder.type;
    FieldSpec cache = context.cache.get();
    return context.lifecycle == REUSE_INSTANCES ?
        statement("$T $N = $N.get().$N", type, builder, cache, mGoal.builderField()) :
        statement("$T $N = new $T()", type, builder, type);
  }

  private static ParameterSpec builderInstance(RegularGoalContext goal) {
    ClassName type = builderImplType(goal);
    return parameterSpec(type, downcase(type.simpleName()));
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
