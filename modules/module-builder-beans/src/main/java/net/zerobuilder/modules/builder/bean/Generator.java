package net.zerobuilder.modules.builder.bean;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;

import java.util.Collections;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class Generator {

  static BuilderMethod builderMethodB(BeanGoalContext goal) {
    String name = goal.details.name;
    MethodSpec method = methodBuilder(BeanBuilder.methodName(goal))
        .returns(BeanBuilder.contractType(goal).nestedClass(upcase(goal.description().parameters().get(0).name())))
        .addModifiers(goal.details.access(STATIC))
        .addExceptions(goal.context.lifecycle == REUSE_INSTANCES
            ? Collections.emptyList()
            : goal.description().thrownTypes)
        .addCode(returnBuilder(goal))
        .build();
    return new BuilderMethod(name, method);
  }

  private static CodeBlock returnBuilder(BeanGoalContext goal) {
    ClassName implType = BeanBuilder.implType(goal);
    ParameterSpec varUpdater = parameterSpec(implType, downcase(implType.simpleName()));
    DtoContext.GoalContext context = goal.context;
    if (goal.context.lifecycle == REUSE_INSTANCES) {
      FieldSpec cache = goal.context.cache.get();
      ParameterSpec varContext = parameterSpec(context.generatedType, "context");
      FieldSpec builderField = BeanBuilder.cacheField(goal);
      return CodeBlock.builder()
          .addStatement("$T $N = $N.get()", varContext.type, varContext, cache)
          .beginControlFlow("if ($N.$N._currently_in_use)", varContext, builderField)
          .addStatement("$N.$N = new $T()", varContext, builderField, implType)
          .endControlFlow()
          .addStatement("$N.$N.$N = new $T()", varContext, varUpdater, goal.bean(), goal.details.goalType)
          .addStatement("$N.$N._currently_in_use = true", varContext, varUpdater)
          .addStatement("return $N.$N", varContext, varUpdater)
          .build();
    }
    return statement("return new $T()", implType);
  }

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
