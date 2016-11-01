package net.zerobuilder.modules.builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoConstructorGoal.SimpleConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoMethodGoal.InstanceMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;

import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularGoalContextCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;

final class GeneratorVB {

  private final Builder builder;

  GeneratorVB(Builder builder) {
    this.builder = builder;
  }

  BuilderMethod builderMethodV(SimpleRegularGoalContext goal) {
    AbstractRegularDetails abstractRegularDetails = goal.regularDetails();
    List<? extends AbstractRegularStep> steps = goal.regularSteps();
    MethodSpec.Builder method = methodBuilder(builder.methodName(goal))
        .returns(builder.contractType(goal).nestedClass(steps.get(0).thisType))
        .addModifiers(abstractRegularDetails.access(STATIC));
    ParameterSpec builder = builderInstance(goal);
    BuildersContext context = goal.context();
    ParameterSpec instance = parameterSpec(context.type, downcase(context.type.simpleName()));
    CodeBlock initStatement = initBuilder(builder, instance).apply(goal);
    method.addCode(initStatement);
    if (goal.isInstance()) {
      method.addParameter(instance);
    }
    MethodSpec methodSpec = method.build();
    return new BuilderMethod(goal.name(), methodSpec);
  }

  private Function<SimpleRegularGoalContext, CodeBlock> initBuilder(
      ParameterSpec builder, ParameterSpec instance) {
    return regularGoalContextCases(
        constructor -> initConstructorBuilder(constructor, builder),
        method -> initInstanceMethodBuilder(method, builder, instance),
        method -> initStaticMethodBuilder(method, builder));
  }

  private CodeBlock initConstructorBuilder(SimpleConstructorGoalContext constructor,
                                           ParameterSpec builder) {
    BuildersContext context = constructor.context;
    TypeName type = builder.type;
    FieldSpec cache = context.cache.get();
    return context.lifecycle == REUSE_INSTANCES ?
        statement("$T $N = $N.get().$N", type, builder, cache, this.builder.cacheField(constructor)) :
        statement("$T $N = new $T()", type, builder, type);
  }

  private CodeBlock initInstanceMethodBuilder(
      InstanceMethodGoalContext method, ParameterSpec builder, ParameterSpec instance) {
    BuildersContext context = method.context;
    TypeName type = builder.type;
    FieldSpec cache = context.cache.get();
    return context.lifecycle == REUSE_INSTANCES ?
        CodeBlock.builder()
            .addStatement("$T $N = $N.get().$N", type, builder, cache, this.builder.cacheField(method))
            .addStatement("$N.$N = $N", builder, method.field(), instance)
            .build() :
        statement("$T $N = new $T($N)", type, builder, type, instance);
  }

  private CodeBlock initStaticMethodBuilder(
      SimpleStaticMethodGoalContext method, ParameterSpec varBuilder) {
    BuildersContext context = method.context;
    FieldSpec cache = context.cache.get();
    if (context.lifecycle == REUSE_INSTANCES) {
      ParameterSpec varContext = parameterSpec(context.generatedType, "context");
      return CodeBlock.builder()
          .addStatement("$T $N = $N.get()", varContext.type, varContext, cache)
          .beginControlFlow("if ($N.refs++ == 0)", varContext)
          .addStatement("return $N.$N", varContext, builder.cacheField(method))
          .endControlFlow()
          .addStatement("return new $T()", varBuilder.type)
          .build();
    }
    return statement("return new $T()", varBuilder.type);
  }

  private ParameterSpec builderInstance(SimpleRegularGoalContext goal) {
    ClassName type = builder.implType(goal);
    return parameterSpec(type, downcase(type.simpleName()));
  }
}
