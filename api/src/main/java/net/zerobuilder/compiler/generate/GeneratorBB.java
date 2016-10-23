package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;

import java.util.Collections;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.Utilities.statement;

final class GeneratorBB {

  private final Builder builder;

  GeneratorBB(Builder builder) {
    this.builder = builder;
  }

  final Function<BeanGoalContext, BuilderMethod> goalToBuilderB
      = goal -> {
    ClassName builderType = goal.implType();
    String name = goal.details.name;
    String builder = downcase(builderType.simpleName());
    ClassName type = goal.details.goalType;
    FieldSpec cache = goal.context.cache.get();
    MethodSpec method = methodBuilder(goal.methodName())
        .returns(goal.contractType().nestedClass(goal.steps().get(0).thisType))
        .addModifiers(goal.details.option.access.modifiers(STATIC))
        .addExceptions(goal.context.lifecycle == REUSE_INSTANCES
            ? Collections.emptyList()
            : goal.thrownTypes)
        .addCode(goal.context.lifecycle == REUSE_INSTANCES
            ? statement("$T $N = $N.get().$N", builderType, builder, cache, goal.cacheField())
            : statement("$T $N = new $T()", builderType, builder, builderType))
        .addCode(goal.context.lifecycle == REUSE_INSTANCES
            ? statement("$N.$N = new $T()", builder, goal.bean(), type)
            : emptyCodeBlock)
        .addStatement("return $N", builder)
        .build();
    return new BuilderMethod(name, method);
  };
}
