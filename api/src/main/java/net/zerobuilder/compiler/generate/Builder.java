package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;

import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoGoalContext.abstractSteps;
import static net.zerobuilder.compiler.generate.DtoGoalContext.builderImplType;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalCases;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalName;
import static net.zerobuilder.compiler.generate.DtoGoalContext.stepInterfaceTypes;
import static net.zerobuilder.compiler.generate.Step.asStepInterface;
import static net.zerobuilder.compiler.generate.Utilities.constructor;
import static net.zerobuilder.compiler.generate.Utilities.transform;

final class Builder {

  private static final Function<AbstractGoalContext, List<FieldSpec>> fields
      = goalCases(BuilderV.fields, BuilderB.fields);

  private static List<TypeSpec> stepInterfaces(AbstractGoalContext goal) {
    return transform(abstractSteps.apply(goal), asStepInterface);
  }

  private static final Function<AbstractGoalContext, List<MethodSpec>> steps
      = goalCases(BuilderV.steps, BuilderB.steps);

  static TypeSpec defineBuilderImpl(AbstractGoalContext goal) {
    return classBuilder(builderImplType(goal))
        .addSuperinterfaces(stepInterfaceTypes(goal))
        .addFields(fields.apply(goal))
        .addMethod(constructor(PRIVATE))
        .addMethods(steps.apply(goal))
        .addModifiers(STATIC, FINAL)
        .build();
  }

  static TypeSpec defineContract(AbstractGoalContext goal) {
    return classBuilder(contractName(goal))
        .addTypes(stepInterfaces(goal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructor(PRIVATE))
        .build();
  }

  private static ClassName contractName(AbstractGoalContext goal) {
    DtoBuildersContext.BuildersContext buildersContext = DtoGoalContext.buildersContext.apply(goal);
    return DtoGoalContext.contractName(goalName.apply(goal), buildersContext.generatedType);
  }

  private Builder() {
    throw new UnsupportedOperationException("no instances");
  }
}
