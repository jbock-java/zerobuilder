package net.zerobuilder.modules.updater;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.InstanceMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static com.palantir.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.concat;
import static net.zerobuilder.compiler.generate.ZeroUtil.constructor;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.updater.Generator.goalMethod;
import static net.zerobuilder.modules.updater.InstanceWorld.factorySpec;
import static net.zerobuilder.modules.updater.Updater.FACTORY;

public final class RegularUpdater implements ProjectedModule {

  static final String moduleName = "updater";

  private CodeBlock regularInvoke(AbstractRegularDetails details, ProjectedRegularGoalDescription description) {
    return switch (details) {
      case ConstructorGoalDetails constructor -> constructorCall(description, constructor);
      case StaticMethodGoalDetails staticMethod -> staticCall(description, staticMethod);
      case InstanceMethodGoalDetails instanceMethod -> instanceCall(description, instanceMethod);
    };
  }

  private final Function<ProjectedRegularGoalDescription, MethodSpec> doneMethod =
      description -> methodBuilder("done")
          .addModifiers(PUBLIC)
          .addExceptions(description.thrownTypes)
          .returns(description.details.type())
          .addCode(regularInvoke(description.details, description))
          .build();

  private TypeSpec defineUpdater(ProjectedRegularGoalDescription description) {
    return classBuilder(simpleName(implType(description)))
        .addFields(Updater.fields(description))
        .addMethods(Updater.stepMethods(description))
        .addTypeVariables(implTypeParameters(description.details))
        .addMethod(doneMethod.apply(description))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructor(PRIVATE))
        .build();
  }

  static TypeName implType(ProjectedRegularGoalDescription description) {
    return parameterizedTypeName(
        description.context.generatedType.nestedClass(implTypeName(description)),
        implTypeParameters(description.details));
  }

  private static String implTypeName(ProjectedRegularGoalDescription description) {
    return upcase(description.details.name()) + upcase(moduleName);
  }

  private static List<TypeVariableName> implTypeParameters(AbstractRegularDetails details) {
    return switch (details) {
      case ConstructorGoalDetails constructor -> constructor.instanceTypeParameters;
      case StaticMethodGoalDetails staticMethod -> List.of();
      case InstanceMethodGoalDetails instanceMethod -> new ArrayList<>(new HashSet<>(concat(
          instanceMethod.instanceTypeParameters,
          instanceMethod.typeParameters)));
    };
  }

  private CodeBlock staticCall(ProjectedRegularGoalDescription description,
                               StaticMethodGoalDetails details) {
    String method = details.methodName;
    TypeName type = details.goalType;
    ParameterSpec varGoal = parameterSpec(type, '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    return builder.addStatement("$T $N = $T.$N($L)", varGoal.type(), varGoal, description.context.type,
            method, details.invocationParameters())
        .addStatement("return $N", varGoal)
        .build();
  }

  private CodeBlock instanceCall(ProjectedRegularGoalDescription description,
                                 InstanceMethodGoalDetails details) {
    String method = details.methodName;
    TypeName type = details.goalType;
    ParameterSpec varGoal = parameterSpec(type, '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    return builder
        .addStatement("$T $N = this.$L.$N($L)", varGoal.type(), varGoal, FACTORY,
            method, details.invocationParameters())
        .addStatement("return $N", varGoal)
        .build();
  }

  private CodeBlock constructorCall(ProjectedRegularGoalDescription description,
                                    ConstructorGoalDetails details) {
    TypeName type = details.goalType;
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    return builder.addStatement("$T $N = new $T($L)", varGoal.type(), varGoal, type,
            details.invocationParameters())
        .addStatement("return $N", varGoal)
        .build();
  }

  static String methodName(ProjectedRegularGoalDescription description) {
    return description.details.name() + upcase(moduleName);
  }

  private List<TypeSpec> types(AbstractRegularDetails details, ProjectedRegularGoalDescription description) {
    return switch (details) {
      case ConstructorGoalDetails constructor -> List.of(defineUpdater(description));
      case StaticMethodGoalDetails staticMethod -> List.of(defineUpdater(description));
      case InstanceMethodGoalDetails instanceMethod -> List.of(
          defineUpdater(description),
          factorySpec(instanceMethod, description));
    };
  }

  @Override
  public ModuleOutput process(ProjectedRegularGoalDescription description) {
    return new ModuleOutput(
        goalMethod(description.details, description),
        types(description.details, description),
        List.of());
  }
}
