package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoModule.ContractModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ContractModuleOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularGoalContextCases;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.abstractSteps;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.simpleGoalCases;
import static net.zerobuilder.compiler.generate.Step.asStepInterface;
import static net.zerobuilder.compiler.generate.ZeroUtil.constructor;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;

public final class Builder extends ContractModule {

  private List<TypeSpec> stepInterfaces(SimpleGoal goal) {
    return transform(abstractSteps.apply(goal), asStepInterface(goal));
  }

  private Function<SimpleGoal, List<MethodSpec>> steps(BuilderB builderB, BuilderV builderV) {
    return simpleGoalCases(builderV.stepsV, builderB.stepsB);
  }

  private Function<SimpleGoal, List<FieldSpec>> fields(BuilderB builderB, BuilderV builderV) {
    return simpleGoalCases(builderV.fieldsV, builderB.fieldsB);
  }

  private Function<SimpleGoal, BuilderMethod> goalToBuilder(GeneratorBB generatorBB, GeneratorVB generatorVB) {
    return simpleGoalCases(generatorVB::goalToBuilderV, generatorBB::goalToBuilderB);
  }

  private TypeSpec defineBuilderImpl(SimpleGoal goal, BuilderB builderB, BuilderV builderV) {
    return classBuilder(implType(goal))
        .addSuperinterfaces(stepInterfaceTypes(goal))
        .addFields(fields(builderB, builderV).apply(goal))
        .addMethod(builderConstructor.apply(goal))
        .addMethods(steps(builderB, builderV).apply(goal))
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
                .addStatement("this.$N = $N", method.field(), parameter)
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
  protected ContractModuleOutput process(SimpleGoal goal) {
    BuilderB builderB = new BuilderB(this);
    BuilderV builderV = new BuilderV(this);
    GeneratorBB generatorBB = new GeneratorBB(this);
    GeneratorVB generatorVB = new GeneratorVB(this);
    return new ContractModuleOutput(
        goalToBuilder(generatorBB, generatorVB).apply(goal),
        defineBuilderImpl(goal, builderB, builderV),
        defineContract(goal));
  }

  @Override
  public String name() {
    return "builder";
  }
}
