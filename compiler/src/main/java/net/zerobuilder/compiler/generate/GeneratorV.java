package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoGoal;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.Utilities.statement;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builderImplType;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.isInstance;
import static net.zerobuilder.compiler.generate.Generator.stepsField;
import static net.zerobuilder.compiler.generate.Generator.updaterField;
import static net.zerobuilder.compiler.generate.UpdaterContext.updaterType;

final class GeneratorV {

  static final Function<RegularGoalContext, MethodSpec> goalToToBuilder =
      new Function<RegularGoalContext, MethodSpec>() {
        @Override
        public MethodSpec apply(RegularGoalContext goal) {
          DtoGoal.RegularGoal regularGoal = DtoRegularGoalContext.regularGoal.apply(goal);
          TypeName goalType = regularGoal.goalType;
          ParameterSpec parameter = parameterSpec(goalType, downcase(((ClassName) goalType.box()).simpleName()));
          String methodName = regularGoal.name + "ToBuilder";
          ParameterSpec updater = updaterInstance(goal);
          MethodSpec.Builder method = methodBuilder(methodName)
              .addParameter(parameter)
              .returns(updater.type)
              .addCode(initializeUpdater(goal, updater));
          for (RegularStep step : goal.steps) {
            method.addCode(copyField(parameter, updater, step));
          }
          method.addStatement("return $N", updater);
          return method.addModifiers(regularGoal.goalOptions.toBuilderAccess.modifiers(STATIC)).build();
        }
      };

  private static CodeBlock copyField(ParameterSpec parameter, ParameterSpec updater, RegularStep step) {
    CodeBlock.Builder builder = CodeBlock.builder();
    String field = step.validParameter.name;
    if (step.validParameter.getter.isPresent()) {
      String getter = step.validParameter.getter.get();
      builder.add(nullCheckGetter(parameter, step))
          .addStatement("$N.$N = $N.$N()",
              updater, field, parameter, getter);
    } else {
      builder.add(nullCheckFieldAccess(parameter, step))
          .addStatement("$N.$N = $N.$N",
              updater, field, parameter, field);
    }
    return builder.build();
  }

  private static CodeBlock nullCheckFieldAccess(ParameterSpec parameter, RegularStep step) {
    if (!step.validParameter.nonNull) {
      return emptyCodeBlock;
    }
    String name = step.validParameter.name;
    return CodeBlock.builder()
        .beginControlFlow("if ($N.$N == null)", parameter, name)
        .addStatement("throw new $T($S)", NullPointerException.class, name)
        .endControlFlow().build();
  }

  private static CodeBlock nullCheckGetter(ParameterSpec parameter, RegularStep step) {
    if (!step.validParameter.nonNull) {
      return emptyCodeBlock;
    }
    String name = step.validParameter.name;
    String getter = step.validParameter.getter.get();
    return CodeBlock.builder()
        .beginControlFlow("if ($N.$N() == null)", parameter, getter)
        .addStatement("throw new $T($S)", NullPointerException.class, name)
        .endControlFlow().build();
  }

  private static CodeBlock initializeUpdater(RegularGoalContext goal, ParameterSpec updater) {
    CodeBlock.Builder builder = CodeBlock.builder();
    DtoBuilders.BuildersContext buildersContext = DtoGoalContext.buildersContext.apply(goal);
    boolean recycle = buildersContext.recycle;
    if (recycle) {
      FieldSpec cache = buildersContext.cache;
      String updaterField = updaterField(goal);
      builder.addStatement("$T $N = $N.get().$N", updater.type, updater, cache, updaterField);
    } else {
      builder.addStatement("$T $N = new $T()", updater.type, updater, updater.type);
    }
    return builder.build();
  }

  private static ParameterSpec updaterInstance(RegularGoalContext goal) {
    ClassName updaterType = updaterType(goal);
    return parameterSpec(updaterType, "updater");
  }


  static final Function<RegularGoalContext, MethodSpec> goalToBuilder
      = new Function<RegularGoalContext, MethodSpec>() {
    @Override
    public MethodSpec apply(RegularGoalContext goal) {
      DtoGoal.RegularGoal regularGoal = DtoRegularGoalContext.regularGoal.apply(goal);
      MethodSpec.Builder method = methodBuilder(DtoGoalContext.goalName.apply(goal) + "Builder")
          .returns(goal.steps.get(0).thisType)
          .addModifiers(regularGoal.goalOptions.builderAccess.modifiers(STATIC));
      ParameterSpec builder = builderInstance(goal);
      method.addCode(initBuilder(goal, builder));
      if (isInstance.apply(goal)) {
        DtoBuilders.BuildersContext buildersContext = DtoRegularGoalContext.buildersContext.apply(goal);
        ParameterSpec parameter = parameterSpec(buildersContext.type,
            downcase(buildersContext.type.simpleName()));
        method.addParameter(parameter)
            .addStatement("$N.$N = $N", builder, buildersContext.field, parameter);
      }
      return method.addStatement("return $N", builder).build();
    }
  };

  private static CodeBlock initBuilder(RegularGoalContext goal, ParameterSpec builder) {
    DtoBuilders.BuildersContext buildersContext = DtoRegularGoalContext.buildersContext.apply(goal);
    return buildersContext.recycle
        ? statement("$T $N = $N.get().$N", builder.type, builder, buildersContext.cache, stepsField(goal))
        : statement("$T $N = new $T()", builder.type, builder, builder.type);
  }

  private static ParameterSpec builderInstance(RegularGoalContext goal) {
    ClassName stepsType = builderImplType(goal);
    return parameterSpec(stepsType, downcase(stepsType.simpleName()));
  }

  private GeneratorV() {
    throw new UnsupportedOperationException("no instances");
  }
}
