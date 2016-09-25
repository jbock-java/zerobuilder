package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.StepContext.BeansStep;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.iterationVarName;
import static net.zerobuilder.compiler.Utilities.statement;
import static net.zerobuilder.compiler.Utilities.upcase;
import static net.zerobuilder.compiler.generate.DtoGoal.builderImplName;
import static net.zerobuilder.compiler.generate.Generator.TL;
import static net.zerobuilder.compiler.generate.Generator.stepsField;
import static net.zerobuilder.compiler.generate.Generator.updaterField;
import static net.zerobuilder.compiler.generate.StepContext.maybeIterationNullCheck;

final class GeneratorB {

  static final Function<BeanGoalContext, MethodSpec> goalToToBuilder
      = new Function<BeanGoalContext, MethodSpec>() {
    @Override
    public MethodSpec apply(BeanGoalContext goal) {
      String instance = downcase(goal.goal.goalType.simpleName());
      String methodName = downcase(goal.goal.name + "ToBuilder");
      CodeBlock.Builder builder = CodeBlock.builder();
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
      builder.addStatement("$N.$N = new $T()", updater, instance, goal.goal.goalType);
      for (BeansStep parameter : goal.steps) {
        String parameterName = upcase(parameter.validBeanParameter.name);
        CodeBlock nullCheck = CodeBlock.builder()
            .beginControlFlow("if ($N.$N() == null)", instance,
                parameter.validBeanParameter.projectionMethodName)
            .addStatement("throw new $T($S)",
                NullPointerException.class, parameter.validBeanParameter.name)
            .endControlFlow().build();
        if (parameter.validBeanParameter.collectionType.isPresent()) {
          TypeName collectionType = parameter.validBeanParameter.collectionType.get();
          builder.add(nullCheck)
              .beginControlFlow("for ($T $N : $N.$N())",
                  collectionType, iterationVarName, instance,
                  parameter.validBeanParameter.projectionMethodName)
              .add(parameter.accept(maybeIterationNullCheck))
              .addStatement("$N.$N.$N().add($N)", updater,
                  downcase(goal.goal.goalType.simpleName()),
                  parameter.validBeanParameter.projectionMethodName,
                  iterationVarName)
              .endControlFlow();
        } else {
          if (parameter.validBeanParameter.nonNull) {
            builder.add(nullCheck);
          }
          builder.addStatement("$N.$N.set$L($N.$N())", updater,
              instance,
              parameterName,
              instance,
              parameter.validBeanParameter.projectionMethodName);
        }
      }
      method.addCode(builder.build());
      method.addStatement("return $L", updater);
      return method
          .returns(goal.accept(UpdaterContext.typeName))
          .addModifiers(PUBLIC, STATIC).build();
    }
  };

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
