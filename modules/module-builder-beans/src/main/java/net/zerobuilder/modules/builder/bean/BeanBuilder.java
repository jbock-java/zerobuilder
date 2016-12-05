package net.zerobuilder.modules.builder.bean;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoModule.BeanModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.bean.BeanStep.beanStepInterface;
import static net.zerobuilder.modules.builder.bean.Builder.fields;
import static net.zerobuilder.modules.builder.bean.Builder.steps;

public final class BeanBuilder implements BeanModule {

  private static final String moduleName = "builder";

  private List<TypeSpec> stepInterfaces(BeanGoalDescription description) {
    return IntStream.range(0, description.parameters.size())
        .mapToObj(beanStepInterface(description))
        .collect(toList());
  }

  static ClassName implType(BeanGoalDescription description) {
    ClassName contract = contractType(description);
    return contract.peerClass(contract.simpleName() + "Impl");
  }

  static String methodName(BeanGoalDescription description) {
    return description.details.name + upcase(moduleName);
  }

  private TypeSpec defineBuilderImpl(BeanGoalDescription description) {
    return classBuilder(implType(description))
        .addSuperinterfaces(stepInterfaceTypes(description))
        .addFields(fields.apply(description))
        .addMethod(builderConstructor.apply(description))
        .addMethods(steps.apply(description))
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build();
  }

  private TypeSpec defineContract(BeanGoalDescription description) {
    return classBuilder(contractType(description))
        .addTypes(stepInterfaces(description))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE)
            .build())
        .build();
  }

  private final Function<BeanGoalDescription, MethodSpec> builderConstructor =
      description -> constructorBuilder()
          .addExceptions(description.thrownTypes)
          .addCode(statement("this.$N = new $T()",
              description.beanField, description.details.goalType))
          .build();

  private List<ClassName> stepInterfaceTypes(BeanGoalDescription description) {
    return transform(description.parameters,
        step -> contractType(description).nestedClass(upcase(step.name())));
  }

  static ClassName contractType(BeanGoalDescription description) {
    String contractName = upcase(description.details.name) + upcase(moduleName);
    return description.details.context.generatedType.nestedClass(contractName);
  }

  @Override
  public ModuleOutput process(BeanGoalDescription description) {
    return new ModuleOutput(
        Generator.builderMethod(description),
        asList(
            defineBuilderImpl(description),
            defineContract(description)),
        emptyList());
  }
}
