package net.zerobuilder.modules.builder;

import io.jbock.javapoet.ClassName;
import io.jbock.javapoet.MethodSpec;
import io.jbock.javapoet.ParameterSpec;
import io.jbock.javapoet.TypeName;
import io.jbock.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoModule.RegularSimpleModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import static io.jbock.javapoet.MethodSpec.constructorBuilder;
import static io.jbock.javapoet.TypeSpec.classBuilder;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoGoalDetails.regularDetailsCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.constructor;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.Builder.fields;
import static net.zerobuilder.modules.builder.Generator.builderMethod;
import static net.zerobuilder.modules.builder.Generator.instanceField;
import static net.zerobuilder.modules.builder.Step.stepInterface;

public final class RegularBuilder implements RegularSimpleModule {

  private static final String moduleName = "builder";

  private List<TypeSpec> stepInterfaces(SimpleRegularGoalDescription description) {
    return IntStream.range(0, description.parameters.size())
        .mapToObj(stepInterface(description))
        .collect(toList());
  }

  private List<MethodSpec> steps(SimpleRegularGoalDescription description) {
    return IntStream.range(0, description.parameters.size())
        .mapToObj(Builder.steps(description))
        .collect(toList());
  }

  static ClassName implType(SimpleRegularGoalDescription description) {
    ClassName contract = contractType(description);
    return contract.peerClass(contract.simpleName() + "Impl");
  }

  static String methodName(SimpleRegularGoalDescription description) {
    return description.details.name + upcase(moduleName);
  }

  private TypeSpec defineBuilderImpl(SimpleRegularGoalDescription description) {
    return classBuilder(implType(description))
        .addSuperinterfaces(stepInterfaceTypes(description))
        .addFields(fields.apply(description))
        .addMethod(regularConstructor.apply(description.details, description))
        .addMethods(steps(description))
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build();
  }

  private TypeSpec defineContract(SimpleRegularGoalDescription description) {
    return classBuilder(contractType(description))
        .addTypes(stepInterfaces(description))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE)
            .build())
        .build();
  }

  private static final BiFunction<AbstractRegularDetails, SimpleRegularGoalDescription, MethodSpec> regularConstructor =
      regularDetailsCases(
          (constructor, description) -> constructor(),
          (staticMethod, description) -> constructor(),
          (method, description) -> {
            if (description.details.lifecycle == REUSE_INSTANCES) {
              return constructor();
            }
            TypeName type = description.context.type;
            ParameterSpec parameter = parameterSpec(type, downcase(simpleName(type)));
            return constructorBuilder()
                .addParameter(parameter)
                .addStatement("this.$N = $N", instanceField(description), parameter)
                .build();
          });

  private List<ClassName> stepInterfaceTypes(SimpleRegularGoalDescription description) {
    return transform(description.parameters,
        step -> contractType(description).nestedClass(upcase(step.name)));
  }

  static ClassName contractType(SimpleRegularGoalDescription description) {
    String contractName = upcase(description.details.name) + upcase(moduleName);
    return description.context.generatedType.nestedClass(contractName);
  }

  @Override
  public ModuleOutput process(SimpleRegularGoalDescription description) {
    return new ModuleOutput(
        builderMethod(description),
        asList(
            defineBuilderImpl(description),
            defineContract(description)),
        description.details.lifecycle == REUSE_INSTANCES ?
            singletonList(description.context.cache(implType(description))) :
            emptyList());
  }
}
