package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoModule.ContractModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ContractModuleOutput;

import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.BuilderB.fieldsB;
import static net.zerobuilder.compiler.generate.BuilderB.stepsB;
import static net.zerobuilder.compiler.generate.BuilderV.fieldsV;
import static net.zerobuilder.compiler.generate.BuilderV.stepsV;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builderConstructor;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalCases;
import static net.zerobuilder.compiler.generate.DtoGoalContext.stepInterfaceTypes;
import static net.zerobuilder.compiler.generate.GeneratorB.goalToBuilderB;
import static net.zerobuilder.compiler.generate.GeneratorVB.goalToBuilderV;
import static net.zerobuilder.compiler.generate.Step.asStepInterface;
import static net.zerobuilder.compiler.generate.Utilities.transform;

public final class Builder extends ContractModule {

  private static final Function<AbstractGoalContext, List<FieldSpec>> fields =
      goalCases(fieldsV, fieldsB);

  private static List<TypeSpec> stepInterfaces(AbstractGoalContext goal) {
    return transform(goal.steps(), asStepInterface);
  }

  private static final Function<AbstractGoalContext, List<MethodSpec>> steps =
      goalCases(stepsV, stepsB);

  private static final Function<AbstractGoalContext, BuilderMethod> goalToBuilder =
      goalCases(goalToBuilderV, goalToBuilderB);

  private static TypeSpec defineBuilderImpl(AbstractGoalContext goal) {
    return classBuilder(goal.implType())
        .addSuperinterfaces(stepInterfaceTypes(goal))
        .addFields(fields.apply(goal))
        .addMethod(builderConstructor.apply(goal))
        .addMethods(steps.apply(goal))
        .addModifiers(STATIC, FINAL)
        .build();
  }

  private static TypeSpec defineContract(AbstractGoalContext goal) {
    return classBuilder(goal.contractType())
        .addTypes(stepInterfaces(goal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE)
            .build())
        .build();
  }

  @Override
  protected ContractModuleOutput process(AbstractGoalContext goal) {
    return new ContractModuleOutput(
        goalToBuilder.apply(goal),
        defineBuilderImpl(goal),
        defineContract(goal));
  }

  @Override
  public String name() {
    return "builder";
  }
}
