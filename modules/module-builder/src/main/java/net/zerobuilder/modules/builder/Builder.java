package net.zerobuilder.modules.builder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoModule.Module;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularGoalContextCases;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.abstractSteps;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.context;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.name;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.simpleGoalCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.constructor;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.Step.asStepInterface;

public final class Builder extends Module {

  private static final String moduleName = "builder";

  private List<TypeSpec> stepInterfaces(SimpleGoal goal) {
    return transform(abstractSteps.apply(goal), asStepInterface(goal));
  }

  private Function<SimpleGoal, List<MethodSpec>> steps =
      simpleGoalCases(BuilderV.stepsV, BuilderB.stepsB);

  private final Function<SimpleGoal, List<FieldSpec>> fields =
      simpleGoalCases(BuilderV.fieldsV, BuilderB.fieldsB);

  private final Function<SimpleGoal, BuilderMethod> goalToBuilder =
      simpleGoalCases(GeneratorV::builderMethodV, GeneratorB::builderMethodB);

  static ClassName implType(SimpleGoal goal) {
    ClassName contract = contractType(goal);
    return contract.peerClass(contract.simpleName() + "Impl");
  }

  static String methodName(SimpleGoal goal) {
    return name.apply(goal) + upcase(moduleName);
  }

  private TypeSpec defineBuilderImpl(SimpleGoal goal) {
    return classBuilder(implType(goal))
        .addSuperinterfaces(stepInterfaceTypes(goal))
        .addFields(fields.apply(goal))
        .addMethod(builderConstructor.apply(goal))
        .addMethods(steps.apply(goal))
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build();
  }

  private TypeSpec defineContract(SimpleGoal goal) {
    return classBuilder(contractType(goal))
        .addTypes(stepInterfaces(goal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE)
            .build())
        .build();
  }

  private static final Function<SimpleRegularGoalContext, MethodSpec> regularConstructor =
      regularGoalContextCases(
          constructor -> constructor(),
          method -> {
            if (method.context.lifecycle == REUSE_INSTANCES) {
              return constructor();
            }
            ClassName type = method.context.type;
            ParameterSpec parameter = parameterSpec(type, downcase(type.simpleName()));
            return constructorBuilder()
                .addParameter(parameter)
                .addStatement("this.$N = $N", method.instanceField(), parameter)
                .build();
          },
          staticMethod -> constructor());

  private final Function<SimpleGoal, MethodSpec> builderConstructor =
      simpleGoalCases(
          regularConstructor,
          bean -> constructorBuilder()
              .addExceptions(bean.context.lifecycle == REUSE_INSTANCES
                  ? Collections.emptyList()
                  : bean.thrownTypes)
              .addCode(bean.context.lifecycle == REUSE_INSTANCES
                  ? emptyCodeBlock
                  : statement("this.$N = new $T()", bean.bean(), bean.type()))
              .build());

  @Override
  protected ModuleOutput process(SimpleGoal goal) {
    return new ModuleOutput(
        goalToBuilder.apply(goal),
        asList(
            defineBuilderImpl(goal),
            defineContract(goal)),
        singletonList(cacheField(goal)));
  }

  static FieldSpec cacheField(SimpleGoal goal) {
    ClassName type = implType(goal);
    return FieldSpec.builder(type, downcase(type.simpleName()), PRIVATE)
        .initializer("new $T()", type)
        .build();
  }


  List<ClassName> stepInterfaceTypes(SimpleGoal goal) {
    return transform(abstractSteps.apply(goal), step -> contractType(goal).nestedClass(step.thisType));
  }

  static ClassName contractType(SimpleGoal goal) {
    String contractName = upcase(name.apply(goal)) + upcase(moduleName);
    return context.apply(goal)
        .generatedType.nestedClass(contractName);
  }
}
