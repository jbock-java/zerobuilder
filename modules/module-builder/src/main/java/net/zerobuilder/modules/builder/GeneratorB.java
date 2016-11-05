package net.zerobuilder.modules.builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoal;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;

import java.util.Collections;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.modules.builder.Builder.cacheField;
import static net.zerobuilder.modules.builder.Builder.contractType;
import static net.zerobuilder.modules.builder.Builder.implType;
import static net.zerobuilder.modules.builder.Builder.methodName;

final class GeneratorB {

  static BuilderMethod builderMethodB(BeanGoalContext goal) {
    String name = goal.details.name;
    MethodSpec method = methodBuilder(methodName(goal))
        .returns(contractType(goal).nestedClass(goal.steps().get(0).thisType))
        .addModifiers(goal.details.access(STATIC))
        .addExceptions(goal.context.lifecycle == REUSE_INSTANCES
            ? Collections.emptyList()
            : goal.thrownTypes)
        .addCode(returnBuilder(goal))
        .build();
    return new BuilderMethod(name, method);
  }

  private static CodeBlock returnBuilder(BeanGoalContext goal) {
    ClassName implType = implType(goal);
    ParameterSpec varUpdater = parameterSpec(implType, downcase(implType.simpleName()));
    DtoContext.BuildersContext context = goal.context;
    if (goal.context.lifecycle == REUSE_INSTANCES) {
      FieldSpec cache = goal.context.cache.get();
      ParameterSpec varContext = parameterSpec(context.generatedType, "context");
      FieldSpec builderField = cacheField(goal);
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

  private GeneratorB() {
    throw new UnsupportedOperationException("no instances");
  }
}
