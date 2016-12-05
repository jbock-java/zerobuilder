package net.zerobuilder.modules.updater;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedInstanceMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.projectedRegularGoalContextCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.concat;
import static net.zerobuilder.compiler.generate.ZeroUtil.constructor;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.updater.Generator.goalMethod;
import static net.zerobuilder.modules.updater.InstanceWorld.factorySpec;

public final class RegularUpdater implements ProjectedModule {

  static final String moduleName = "updater";

  private final Function<ProjectedRegularGoalContext, CodeBlock> regularInvoke =
      projectedRegularGoalContextCases(
          this::staticCall,
          this::instanceCall,
          this::constructorCall);

  private final Function<ProjectedRegularGoalContext, MethodSpec> doneMethod =
      goal -> methodBuilder("done")
          .addModifiers(PUBLIC)
          .addExceptions(goal.description.thrownTypes)
          .returns(goal.description.details.type())
          .addCode(regularInvoke.apply(goal))
          .build();

  private TypeSpec defineUpdater(ProjectedRegularGoalContext projectedGoal) {
    return classBuilder(simpleName(implType(projectedGoal)))
        .addFields(Updater.fields(projectedGoal))
        .addMethods(Updater.stepMethods(projectedGoal))
        .addTypeVariables(implTypeParameters.apply(projectedGoal))
        .addMethod(doneMethod.apply(projectedGoal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructor(PRIVATE))
        .build();
  }

  static TypeName implType(ProjectedRegularGoalContext goal) {
    return parameterizedTypeName(
        goal.description.context.generatedType.nestedClass(implTypeName(goal)),
        implTypeParameters.apply(goal));
  }

  private static String implTypeName(ProjectedRegularGoalContext goal) {
    return upcase(goal.description.details.name()) + upcase(moduleName);
  }

  private static final Function<ProjectedRegularGoalContext, Collection<TypeVariableName>> implTypeParameters =
      projectedRegularGoalContextCases(
          staticMethod -> emptyList(),
          instanceMethod -> new HashSet<>(concat(
              instanceMethod.details.instanceTypeParameters,
              instanceMethod.details.typeParameters)),
          constructor -> constructor.details.instanceTypeParameters);

  private CodeBlock staticCall(ProjectedMethodGoalContext goal) {
    String method = goal.details.methodName;
    TypeName type = goal.details.goalType;
    ParameterSpec varGoal = parameterSpec(type, '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (isReusable.apply(goal)) {
      builder.addStatement("this._currently_in_use = false");
    }
    builder.addStatement("$T $N = $T.$N($L)", varGoal.type, varGoal, goal.description.context.type,
        method, goal.description.invocationParameters());
    if (isReusable.apply(goal)) {
      builder.add(free(goal.description.parameters));
    }
    return builder.addStatement("return $N", varGoal)
        .build();
  }

  private CodeBlock instanceCall(ProjectedInstanceMethodGoalContext goal) {
    String method = goal.details.methodName;
    TypeName type = goal.details.goalType;
    ParameterSpec varGoal = parameterSpec(type, '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (isReusable.apply(goal)) {
      builder.addStatement("this._currently_in_use = false");
    }
    return builder
        .addStatement("$T $N = _factory.$N($L)", varGoal.type, varGoal,
            method, goal.description.invocationParameters())
        .addStatement("return $N", varGoal)
        .build();
  }

  private CodeBlock constructorCall(ProjectedConstructorGoalContext goal) {
    TypeName type = goal.details.goalType;
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (isReusable.apply(goal)) {
      builder.addStatement("this._currently_in_use = false");
    }
    builder.addStatement("$T $N = new $T($L)", varGoal.type, varGoal, type, goal.description.invocationParameters());
    if (isReusable.apply(goal)) {
      builder.add(free(goal.description.parameters));
    }
    return builder.addStatement("return $N", varGoal)
        .build();
  }

  private CodeBlock free(List<ProjectedParameter> steps) {
    return steps.stream()
        .filter(parameter -> !parameter.type.isPrimitive())
        .map(parameter -> statement("this.$N = null", parameter.name))
        .collect(joinCodeBlocks);
  }

  static String methodName(ProjectedRegularGoalContext goal) {
    return goal.description.details.name() + upcase(moduleName);
  }

  static final Function<ProjectedRegularGoalContext, Boolean> isReusable =
      projectedRegularGoalContextCases(
          staticMethod -> staticMethod.details.lifecycle == REUSE_INSTANCES &&
              staticMethod.details.typeParameters.isEmpty(),
          instanceMethod -> instanceMethod.details.lifecycle == REUSE_INSTANCES &&
              instanceMethod.details.typeParameters.isEmpty() &&
              instanceMethod.details.instanceTypeParameters.isEmpty() &&
              instanceMethod.details.returnTypeParameters.isEmpty(),
          constructor -> constructor.details.lifecycle == REUSE_INSTANCES &&
              constructor.details.instanceTypeParameters.isEmpty());

  private final Function<ProjectedRegularGoalContext, List<TypeSpec>> types =
      projectedRegularGoalContextCases(
          staticMethod -> singletonList(defineUpdater(staticMethod)),
          instanceMethod -> asList(defineUpdater(instanceMethod), factorySpec(instanceMethod)),
          constructor -> singletonList(defineUpdater(constructor)));

  @Override
  public ModuleOutput process(ProjectedRegularGoalContext goal) {
    return new ModuleOutput(
        goalMethod.apply(goal),
        types.apply(goal),
        isReusable.apply(goal) ?
            singletonList(goal.description.context.cache(implTypeName(goal))) :
            emptyList());
  }
}
