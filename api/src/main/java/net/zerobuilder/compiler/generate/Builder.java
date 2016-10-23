package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoModule.ContractModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ContractModuleOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoal.AbstractRegularGoalContext;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Arrays.asList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalCases;
import static net.zerobuilder.compiler.generate.Step.asStepInterface;
import static net.zerobuilder.compiler.generate.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.Utilities.statement;
import static net.zerobuilder.compiler.generate.Utilities.transform;

public final class Builder extends ContractModule {

  private List<TypeSpec> stepInterfaces(AbstractGoalContext goal) {
    return transform(goal.steps(), asStepInterface);
  }

  private final Function<AbstractGoalContext, List<MethodSpec>> steps(BuilderB builderB, BuilderV builderV) {
    return goalCases(builderV.stepsV, builderB.stepsB);
  }

  private final Function<AbstractGoalContext, List<FieldSpec>> fields(BuilderB builderB, BuilderV builderV) {
    return goalCases(builderV.fieldsV, builderB.fieldsB);
  }

  private final Function<AbstractGoalContext, BuilderMethod> goalToBuilder(GeneratorBB generatorBB, GeneratorVB generatorVB) {
    return goalCases(generatorVB::goalToBuilderV, generatorBB::goalToBuilderB);
  }

  private TypeSpec defineBuilderImpl(AbstractGoalContext goal, BuilderB builderB, BuilderV builderV) {
    return classBuilder(implType(goal))
        .addSuperinterfaces(stepInterfaceTypes(goal))
        .addFields(fields(builderB, builderV).apply(goal))
        .addMethod(builderConstructor.apply(goal))
        .addMethods(steps(builderB, builderV).apply(goal))
        .addModifiers(STATIC, FINAL)
        .build();
  }

  private TypeSpec defineContract(AbstractGoalContext goal) {
    return classBuilder(contractType(goal))
        .addTypes(stepInterfaces(goal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE)
            .build())
        .build();
  }

  private final Function<AbstractGoalContext, MethodSpec> builderConstructor =
      goalCases(
          AbstractRegularGoalContext::builderConstructor,
          bGoal -> constructorBuilder()
              .addModifiers(PRIVATE)
              .addExceptions(bGoal.context.lifecycle == REUSE_INSTANCES
                  ? Collections.emptyList()
                  : bGoal.thrownTypes)
              .addCode(bGoal.context.lifecycle == REUSE_INSTANCES
                  ? emptyCodeBlock
                  : statement("this.$N = new $T()", bGoal.bean(), bGoal.type()))
              .build());

  @Override
  protected ContractModuleOutput process(AbstractGoalContext goal) {
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
