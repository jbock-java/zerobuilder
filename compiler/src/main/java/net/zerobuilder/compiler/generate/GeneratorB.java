package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.BeanStep;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.Utilities.statement;
import static net.zerobuilder.compiler.generate.DtoGoal.builderImplName;
import static net.zerobuilder.compiler.generate.Generator.TL;
import static net.zerobuilder.compiler.generate.Generator.stepsField;
import static net.zerobuilder.compiler.generate.Generator.updaterField;
import static net.zerobuilder.compiler.generate.StepContext.iterationVarNullCheck;

final class GeneratorB {

  static final Function<BeanGoalContext, MethodSpec> goalToToBuilder
      = new Function<BeanGoalContext, MethodSpec>() {
    @Override
    public MethodSpec apply(BeanGoalContext goal) {
      String methodName = downcase(goal.goal.name + "ToBuilder");
      CodeBlock.Builder builder = CodeBlock.builder();
      ParameterSpec parameter = parameterSpec(goal.goal.goalType, goal.field.name);
      MethodSpec.Builder method = methodBuilder(methodName)
          .addParameter(parameter);
      ParameterSpec updater = updaterInstance(goal);
      method.addCode(initializeUpdater(goal, updater));
      builder.addStatement("$N.$N = new $T()", updater, goal.field, goal.goal.goalType);
      for (BeanStep step : goal.steps) {
        CodeBlock nullCheck = CodeBlock.builder()
            .beginControlFlow("if ($N.$N() == null)", parameter,
                step.validParameter.getter)
            .addStatement("throw new $T($S)",
                NullPointerException.class, step.validParameter.name)
            .endControlFlow().build();
        if (step.validParameter.collectionType.isPresent()) {
          ParameterSpec iterationVar = step.validParameter.collectionType.get();
          builder.add(nullCheck)
              .beginControlFlow("for ($T $N : $N.$N())",
                  iterationVar.type, iterationVar, parameter,
                  step.validParameter.getter)
              .add(iterationVarNullCheck(step))
              .addStatement("$N.$N.$N().add($N)", updater,
                  downcase(goal.goal.goalType.simpleName()),
                  step.validParameter.getter,
                  iterationVar)
              .endControlFlow();
        } else {
          if (step.validParameter.nonNull) {
            builder.add(nullCheck);
          }
          builder.addStatement("$N.$N.$L($N.$N())", updater,
              goal.field,
              step.setter,
              parameter,
              step.validParameter.getter);
        }
      }
      method.addCode(builder.build());
      method.addStatement("return $N", updater);
      return method
          .returns(goal.accept(UpdaterContext.typeName))
          .addModifiers(PUBLIC, STATIC).build();
    }
  };

  private static CodeBlock initializeUpdater(BeanGoalContext goal, ParameterSpec updater) {
    if (goal.builders.recycle) {
      return statement("$T $N = $L.get().$N", updater.type, updater,
          TL, updaterField(goal));
    } else {
      return statement("$T $N = new $T()", updater.type, updater, updater.type);
    }
  }

  private static ParameterSpec updaterInstance(BeanGoalContext goal) {
    ClassName updaterType = goal.accept(UpdaterContext.typeName);
    return parameterSpec(updaterType, "updater");
  }

  static final Function<BeanGoalContext, MethodSpec> goalToBuilder
      = new Function<BeanGoalContext, MethodSpec>() {
    @Override
    public MethodSpec apply(BeanGoalContext goal) {
      ClassName stepsType = goal.accept(builderImplName);
      MethodSpec.Builder method = methodBuilder(goal.goal.name + "Builder")
          .returns(goal.steps.get(0).thisType)
          .addModifiers(PUBLIC, STATIC);
      String steps = downcase(stepsType.simpleName());
      method.addCode(goal.builders.recycle
          ? statement("$T $N = $N.get().$N", stepsType, steps, TL, stepsField(goal))
          : statement("$T $N = new $T()", stepsType, steps, stepsType));
      return method.addStatement("$N.$N = new $T()", steps,
          downcase(goal.goal.goalType.simpleName()), goal.goal.goalType)
          .addStatement("return $N", steps)
          .build();
    }
  };

  private GeneratorB() {
    throw new UnsupportedOperationException("no instances");
  }
}
