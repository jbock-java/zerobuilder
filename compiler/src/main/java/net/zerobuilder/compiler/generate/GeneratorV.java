package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.Utilities.statement;
import static net.zerobuilder.compiler.analyse.GoalContextFactory.GoalKind.INSTANCE_METHOD;
import static net.zerobuilder.compiler.generate.DtoGoal.builderImplName;
import static net.zerobuilder.compiler.generate.DtoGoal.getGoalName;
import static net.zerobuilder.compiler.generate.Generator.TL;
import static net.zerobuilder.compiler.generate.Generator.stepsField;
import static net.zerobuilder.compiler.generate.Generator.updaterField;

final class GeneratorV {

  static final Function<RegularGoalContext, MethodSpec> goalToToBuilder =
      new Function<RegularGoalContext, MethodSpec>() {
        @Override
        public MethodSpec apply(RegularGoalContext goal) {
          ParameterSpec parameter = parameterSpec(goal.goal.goalType,
              downcase(((ClassName) goal.goal.goalType.box()).simpleName()));
          String methodName = goal.goal.name + "ToBuilder";
          ParameterSpec updater = updaterInstance(goal);
          MethodSpec.Builder method = methodBuilder(methodName)
              .addParameter(parameter)
              .returns(updater.type)
              .addCode(initializeUpdater(goal, updater));
          for (RegularStep step : goal.steps) {
            method.addCode(copyField(parameter, updater, step));
          }
          method.addStatement("return $N", updater);
          return method.addModifiers(PUBLIC, STATIC).build();
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
    if (goal.builders.recycle) {
      builder.addStatement("$T $N = $N.get().$N", updater.type, updater,
          TL, updaterField(goal));
    } else {
      builder.addStatement("$T $N = new $T()", updater.type, updater, updater.type);
    }
    return builder.build();
  }

  private static ParameterSpec updaterInstance(RegularGoalContext goal) {
    ClassName updaterType = goal.accept(UpdaterContext.typeName);
    return parameterSpec(updaterType, "updater");
  }


  static final Function<RegularGoalContext, MethodSpec> goalToBuilder
      = new Function<RegularGoalContext, MethodSpec>() {
    @Override
    public MethodSpec apply(RegularGoalContext goal) {
      ClassName stepsType = goal.accept(builderImplName);
      MethodSpec.Builder method = methodBuilder(goal.accept(getGoalName) + "Builder")
          .returns(goal.steps.get(0).thisType)
          .addModifiers(PUBLIC, STATIC);
      String steps = downcase(stepsType.simpleName());
      method.addCode(goal.builders.recycle
          ? statement("$T $N = $N.get().$N", stepsType, steps, TL, stepsField(goal))
          : statement("$T $N = new $T()", stepsType, steps, stepsType));
      if (goal.goal.kind == INSTANCE_METHOD) {
        ClassName instanceType = goal.builders.type;
        String instance = downcase(instanceType.simpleName());
        return method
            .addParameter(parameterSpec(instanceType, instance))
            .addStatement("$N.$N = $N", steps, goal.builders.field, instance)
            .addStatement("return $N", steps)
            .build();
      } else {
        return method.addStatement("return $N", steps).build();
      }
    }
  };

  private GeneratorV() {
    throw new UnsupportedOperationException("no instances");
  }
}
