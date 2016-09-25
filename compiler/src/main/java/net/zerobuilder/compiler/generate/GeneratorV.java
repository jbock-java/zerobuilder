package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalContext;
import net.zerobuilder.compiler.generate.StepContext.RegularStep;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Utilities.downcase;
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
          String instance = downcase(((ClassName) goal.goal.goalType.box()).simpleName());
          String methodName = goal.goal.name + "ToBuilder";
          MethodSpec.Builder method = methodBuilder(methodName)
              .addParameter(goal.goal.goalType, instance);
          String updater = "updater";
          ClassName updaterType = goal.accept(UpdaterContext.typeName);
          if (goal.builders.recycle) {
            method.addStatement("$T $L = $L.get().$N", updaterType, updater,
                TL, updaterField(goal));
          } else {
            method.addStatement("$T $L = new $T()", updaterType, updater,
                updaterType);
          }
          CodeBlock.Builder builder = CodeBlock.builder();
          for (RegularStep parameter : goal.steps) {
            if (parameter.parameter.projectionMethodName.isPresent()) {
              if (parameter.parameter.nonNull) {
                builder.add(CodeBlock.builder()
                    .beginControlFlow("if ($N.$N() == null)", instance, parameter.parameter.projectionMethodName.get())
                    .addStatement("throw new $T($S)", NullPointerException.class, parameter.parameter.name)
                    .endControlFlow().build());
              }
              builder.addStatement("$N.$N = $N.$N()", updater, parameter.parameter.name,
                  instance, parameter.parameter.projectionMethodName.get());
            } else {
              if (parameter.parameter.nonNull) {
                builder.add(CodeBlock.builder()
                    .beginControlFlow("if ($N.$N == null)", instance, parameter.parameter.name)
                    .addStatement("throw new $T($S)", NullPointerException.class, parameter.parameter.name)
                    .endControlFlow().build());
              }
              builder.add(CodeBlock.builder()
                  .addStatement("$N.$N = $N.$N", updater, parameter.parameter.name,
                      instance, parameter.parameter.name).build());
            }
          }
          method.addCode(builder.build());
          method.addStatement("return $L", updater);
          return method
              .returns(goal.accept(UpdaterContext.typeName))
              .addModifiers(PUBLIC, STATIC).build();
        }
      };

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
