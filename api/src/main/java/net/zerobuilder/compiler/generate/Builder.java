package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;

import java.util.Arrays;
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
import static net.zerobuilder.compiler.generate.DtoGoalContext.abstractSteps;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builderConstructor;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builderImplType;
import static net.zerobuilder.compiler.generate.DtoGoalContext.buildersContext;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalCases;
import static net.zerobuilder.compiler.generate.DtoGoalContext.stepInterfaceTypes;
import static net.zerobuilder.compiler.generate.GeneratorB.goalToBuilderB;
import static net.zerobuilder.compiler.generate.GeneratorV.goalToBuilderV;
import static net.zerobuilder.compiler.generate.Step.asStepInterface;
import static net.zerobuilder.compiler.generate.Utilities.transform;
import static net.zerobuilder.compiler.generate.Utilities.upcase;

public final class Builder implements Generator.Module {

  static final String MODULE_NAME = "builder";

  private static final Function<AbstractGoalContext, List<FieldSpec>> fields =
      goalCases(fieldsV, fieldsB);

  private static List<TypeSpec> stepInterfaces(AbstractGoalContext goal) {
    return transform(abstractSteps.apply(goal), asStepInterface);
  }

  private static final Function<AbstractGoalContext, List<MethodSpec>> steps =
      goalCases(stepsV, stepsB);

  private static final Function<AbstractGoalContext, BuilderMethod> goalToBuilder =
      goalCases(goalToBuilderV, goalToBuilderB);

  static ClassName contractName(AbstractGoalContext goal) {
    BuildersContext context = buildersContext.apply(goal);
    return context.generatedType.nestedClass(upcase(goal.name() + "Builder"));
  }

  private static TypeSpec defineBuilderImpl(AbstractGoalContext goal) {
    return classBuilder(builderImplType(goal))
        .addSuperinterfaces(stepInterfaceTypes(goal))
        .addFields(fields.apply(goal))
        .addMethod(builderConstructor.apply(goal))
        .addMethods(steps.apply(goal))
        .addModifiers(STATIC, FINAL)
        .build();
  }

  private static TypeSpec defineContract(AbstractGoalContext goal) {
    return classBuilder(contractName(goal))
        .addTypes(stepInterfaces(goal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE)
            .build())
        .build();
  }

  @Override
  public BuilderMethod method(AbstractGoalContext goal) {
    return goalToBuilder.apply(goal);
  }

  @Override
  public List<TypeSpec> nestedTypes(AbstractGoalContext goal) {
    return Arrays.asList(
        defineBuilderImpl(goal),
        defineContract(goal));
  }

  @Override
  public FieldSpec field(AbstractGoalContext goal) {
    return goal.builderField();
  }

  @Override
  public boolean needsProjections() {
    return false;
  }

  @Override
  public String name() {
    return MODULE_NAME;
  }
}
