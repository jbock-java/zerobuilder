package net.zerobuilder.modules.builder.bean;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;

import java.util.Collections;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.bean.BeanBuilder.implType;

final class Generator {

  static BuilderMethod builderMethod(BeanGoalContext goal) {
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
    ClassName implType = implType(goal);
    GoalContext context = goal.context;
    if (context.lifecycle == REUSE_INSTANCES) {
      ParameterSpec varUpdater = parameterSpec(implType, downcase("_builder"));
      FieldSpec cache = context.cache(implType);
      return CodeBlock.builder()
          .addStatement("$T $N = $N.get()", varUpdater.type, varUpdater, cache)
          .beginControlFlow("if ($N._currently_in_use)", varUpdater)
          .addStatement("$N.remove()", cache)
          .addStatement("$N = $N.get()", varUpdater, cache)
          .endControlFlow()
          .addStatement("$N.$N = new $T()", varUpdater, goal.bean(), goal.details.goalType)
          .addStatement("$N._currently_in_use = true", varUpdater)
          .addStatement("return $N", varUpdater)
          .build();
    }
    return statement("return new $T()", implType);
  }

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
