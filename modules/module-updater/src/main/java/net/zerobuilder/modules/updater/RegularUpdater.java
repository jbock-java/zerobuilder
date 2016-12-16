package net.zerobuilder.modules.updater;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.InstanceMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.NullPolicy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiFunction;
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
import static net.zerobuilder.compiler.generate.DtoGoalDetails.regularDetailsCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.concat;
import static net.zerobuilder.compiler.generate.ZeroUtil.constructor;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
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

  private final BiFunction<AbstractRegularDetails, ProjectedRegularGoalDescription, CodeBlock> regularInvoke =
      regularDetailsCases(
          (constructor, description) -> constructorCall(description, constructor),
          (staticMethod, description) -> staticCall(description, staticMethod),
          (instanceMethod, description) -> instanceCall(description, instanceMethod));

  private static CodeBlock nullCheck(ProjectedParameter step) {
    if (step.nullPolicy == NullPolicy.ALLOW) {
      return emptyCodeBlock;
    }
    return CodeBlock.builder()
        .beginControlFlow("if (this.$N == null)", step.name)
        .addStatement("throw new $T($S)",
            NullPointerException.class, step.name)
        .endControlFlow().build();
  }

  private final Function<ProjectedRegularGoalDescription, MethodSpec> doneMethod =
      description -> methodBuilder("done")
          .addModifiers(PUBLIC)
          .addExceptions(description.thrownTypes)
          .returns(description.details.type())
          .addCode(description.parameters.stream()
              .filter(parameter -> parameter.nullPolicy == NullPolicy.REJECT)
              .map(RegularUpdater::nullCheck)
              .collect(joinCodeBlocks))
          .addCode(regularInvoke.apply(description.details, description))
          .build();

  private TypeSpec defineUpdater(ProjectedRegularGoalDescription description) {
    return classBuilder(simpleName(implType(description)))
        .addFields(Updater.fields(description))
        .addMethods(Updater.stepMethods(description))
        .addTypeVariables(implTypeParameters.apply(description.details))
        .addMethod(doneMethod.apply(description))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructor(PRIVATE))
        .build();
  }

  static TypeName implType(ProjectedRegularGoalDescription description) {
    return parameterizedTypeName(
        description.context.generatedType.nestedClass(implTypeName(description)),
        implTypeParameters.apply(description.details));
  }

  private static String implTypeName(ProjectedRegularGoalDescription description) {
    return upcase(description.details.name()) + upcase(moduleName);
  }

  private static final Function<AbstractRegularDetails, List<TypeVariableName>> implTypeParameters =
      regularDetailsCases(
          constructor -> constructor.instanceTypeParameters,
          staticMethod -> emptyList(),
          instanceMethod -> new ArrayList<>(new HashSet<>(concat(
              instanceMethod.instanceTypeParameters,
              instanceMethod.typeParameters))));

  private CodeBlock staticCall(ProjectedRegularGoalDescription description,
                               StaticMethodGoalDetails details) {
    String method = details.methodName;
    TypeName type = details.goalType;
    ParameterSpec varGoal = parameterSpec(type, '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (isReusable.apply(details)) {
      builder.addStatement("this._currently_in_use = false");
    }
    return builder.addStatement("$T $N = $T.$N($L)", varGoal.type, varGoal, description.context.type,
        method, details.invocationParameters())
        .add(free(description))
        .addStatement("return $N", varGoal)
        .build();
  }

  private CodeBlock instanceCall(ProjectedRegularGoalDescription description,
                                 InstanceMethodGoalDetails details) {
    String method = details.methodName;
    TypeName type = details.goalType;
    ParameterSpec varGoal = parameterSpec(type, '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (isReusable.apply(details)) {
      builder.addStatement("this._currently_in_use = false");
    }
    return builder
        .addStatement("$T $N = _factory.$N($L)", varGoal.type, varGoal,
            method, details.invocationParameters())
        .add(free(description))
        .addStatement("return $N", varGoal)
        .build();
  }

  private CodeBlock constructorCall(ProjectedRegularGoalDescription description,
                                    ConstructorGoalDetails details) {
    TypeName type = details.goalType;
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (isReusable.apply(details)) {
      builder.addStatement("this._currently_in_use = false");
    }
    return builder.addStatement("$T $N = new $T($L)", varGoal.type, varGoal, type,
        details.invocationParameters())
        .add(free(description))
        .addStatement("return $N", varGoal)
        .build();
  }

  private CodeBlock free(ProjectedRegularGoalDescription description) {
    if (!isReusable.apply(description.details)) {
      return emptyCodeBlock;
    }
    return description.parameters.stream()
        .filter(parameter -> !parameter.type.isPrimitive())
        .map(parameter -> statement("this.$N = null", parameter.name))
        .collect(joinCodeBlocks);
  }

  static String methodName(ProjectedRegularGoalDescription description) {
    return description.details.name() + upcase(moduleName);
  }

  static final Function<AbstractRegularDetails, Boolean> isReusable =
      regularDetailsCases(
          RegularUpdater::isConstructorReusable,
          RegularUpdater::isStaticMethodReusable,
          RegularUpdater::isInstanceMethodReusable);

  private static boolean isInstanceMethodReusable(InstanceMethodGoalDetails details) {
    return details.lifecycle == REUSE_INSTANCES &&
        details.typeParameters.isEmpty() &&
        details.instanceTypeParameters.isEmpty() &&
        details.returnTypeParameters.isEmpty();
  }

  private static boolean isStaticMethodReusable(StaticMethodGoalDetails details) {
    return details.lifecycle == REUSE_INSTANCES &&
        details.typeParameters.isEmpty();
  }

  private static boolean isConstructorReusable(ConstructorGoalDetails details) {
    return details.lifecycle == REUSE_INSTANCES &&
        details.instanceTypeParameters.isEmpty();
  }

  private final BiFunction<AbstractRegularDetails, ProjectedRegularGoalDescription, List<TypeSpec>> types =
      regularDetailsCases(
          (constructor, description) -> singletonList(defineUpdater(description)),
          (staticMethod, description) -> singletonList(defineUpdater(description)),
          (instanceMethod, description) -> asList(
              defineUpdater(description),
              factorySpec(instanceMethod, description)));

  @Override
  public ModuleOutput process(ProjectedRegularGoalDescription description) {
    return new ModuleOutput(
        goalMethod.apply(description.details, description),
        types.apply(description.details, description),
        isReusable.apply(description.details) ?
            singletonList(description.context.cache(implTypeName(description))) :
            emptyList());
  }
}
