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

final class GeneratorB {

  private final Builder builder;

  GeneratorB(Builder builder) {
    this.builder = builder;
  }

  BuilderMethod builderMethodB(BeanGoalContext goal) {
    String name = goal.details.name;
    MethodSpec method = methodBuilder(this.builder.methodName(goal))
        .returns(builder.contractType(goal).nestedClass(goal.steps().get(0).thisType))
        .addModifiers(goal.details.access(STATIC))
        .addExceptions(goal.context.lifecycle == REUSE_INSTANCES
            ? Collections.emptyList()
            : goal.thrownTypes)
        .addCode(returnBuilder(goal))
        .build();
    return new BuilderMethod(name, method);
  }

  private CodeBlock returnBuilder(BeanGoalContext goal) {
    ClassName implType = builder.implType(goal);
    ParameterSpec varBuilder = parameterSpec(implType, downcase(implType.simpleName()));
    DtoContext.BuildersContext context = goal.context;
    if (goal.context.lifecycle == REUSE_INSTANCES) {
      FieldSpec cache = goal.context.cache.get();
      ParameterSpec varContext = parameterSpec(context.generatedType, "context");
      return CodeBlock.builder()
          .addStatement("$T $N = $N.get()", varContext.type, varContext, cache)
          .addStatement("$T $N", implType, varBuilder)
          .beginControlFlow("if ($N.refs++ == 0)", varContext)
          .addStatement("$N = $N.$N", varBuilder, varContext, builder.cacheField(goal))
          .endControlFlow()
          .beginControlFlow("else")
          .addStatement("$N = new $T()", varBuilder, implType)
          .endControlFlow()
          .addStatement("$N.$N = new $T()", varBuilder, goal.bean(), goal.details.goalType)
          .addStatement("return $N", varBuilder)
          .build();
    }
    return statement("return new $T()", implType);
  }
}
