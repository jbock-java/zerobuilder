package net.zerobuilder.modules.builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
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

final class GeneratorV {

  private final Builder builder;

  GeneratorV(Builder builder) {
    this.builder = builder;
  }

  BuilderMethod builderMethodV(SimpleRegularGoalContext goal) {
    AbstractRegularDetails abstractRegularDetails = goal.regularDetails();
    List<? extends AbstractRegularStep> steps = goal.regularSteps();
    MethodSpec.Builder method = methodBuilder(builder.methodName(goal))
        .returns(builder.contractType(goal).nestedClass(steps.get(0).thisType))
        .addModifiers(abstractRegularDetails.access(STATIC));
    BuildersContext context = goal.context();
    ParameterSpec varInstance = parameterSpec(context.type,
        downcase(context.type.simpleName()));
    CodeBlock returnBlock = returnBlock(varInstance).apply(goal);
    method.addCode(returnBlock);
    if (goal.isInstance()) {
      method.addParameter(varInstance);
    }
    MethodSpec methodSpec = method.build();
    return new BuilderMethod(goal.name(), methodSpec);
  }

  private Function<SimpleRegularGoalContext, CodeBlock> returnBlock(ParameterSpec varInstance) {
    return regularGoalContextCases(
        this::returnConstructor,
        method -> returnInstanceMethod(method, varInstance),
        this::returnStaticMethod);
  }

  private CodeBlock returnConstructor(SimpleConstructorGoalContext goal) {
    ParameterSpec varBuilder = builderInstance(goal);
    BuildersContext context = goal.context;
    if (context.lifecycle == REUSE_INSTANCES) {
      FieldSpec cache = context.cache.get();
      ParameterSpec varContext = parameterSpec(context.generatedType, "context");
      return CodeBlock.builder()
          .addStatement("$T $N = $N.get()", varContext.type, varContext, cache)
          .beginControlFlow("if ($N.refs++ == 0)", varContext)
          .addStatement("return $N.$N", varContext, builder.cacheField(goal))
          .endControlFlow()
          .addStatement("return new $T()", varBuilder.type)
          .build();
    }
    return statement("return new $T()", varBuilder.type);
  }

  private CodeBlock returnInstanceMethod(
      InstanceMethodGoalContext goal, ParameterSpec varInstance) {
    ParameterSpec varBuilder = builderInstance(goal);
    BuildersContext context = goal.context;
    if (context.lifecycle == REUSE_INSTANCES) {
      FieldSpec cache = context.cache.get();
      ParameterSpec varContext = parameterSpec(context.generatedType, "context");
      return CodeBlock.builder()
          .addStatement("$T $N = $N.get()", varContext.type, varContext, cache)
          .addStatement("$T $N", varBuilder.type, varBuilder)
          .beginControlFlow("if ($N.refs++ == 0)", varContext)
          .addStatement("$N = $N.$N", varBuilder, varContext, builder.cacheField(goal))
          .endControlFlow()
          .beginControlFlow("else")
          .addStatement("$N = new $T()", varBuilder, varBuilder.type)
          .endControlFlow()
          .addStatement("$N.$N = $N", varBuilder, goal.field(), varInstance)
          .addStatement("return $N", varBuilder)
          .build();
    }
    return statement("return new $T($N)", varBuilder.type, varInstance);
  }

  private CodeBlock returnStaticMethod(
      SimpleStaticMethodGoalContext goal) {
    ParameterSpec varBuilder = builderInstance(goal);
    BuildersContext context = goal.context;
    if (context.lifecycle == REUSE_INSTANCES) {
      FieldSpec cache = context.cache.get();
      ParameterSpec varContext = parameterSpec(context.generatedType, "context");
      FieldSpec goalField = builder.cacheField(goal);
      return CodeBlock.builder()
          .addStatement("$T $N = $N.get()", varContext.type, varContext, cache)
          .beginControlFlow("if ($N.$N._currently_in_use)", varContext, goalField)
          .addStatement("$N.$N = new $T()", varContext, goalField, varBuilder.type)
          .endControlFlow()
          .addStatement("$N.$N._currently_in_use = true", varContext, goalField)
          .addStatement("return $N.$N", varContext, goalField)
          .build();
    }
    return statement("return new $T()", varBuilder.type);
  }

  private ParameterSpec builderInstance(SimpleRegularGoalContext goal) {
    ClassName type = builder.implType(goal);
    return parameterSpec(type, downcase(type.simpleName()));
  }
}
