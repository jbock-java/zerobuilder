package net.zerobuilder.modules.generics;

import com.squareup.javapoet.MethodSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.STATIC;

public final class GenericsBuilder extends DtoModule.RegularContractModule {

  private static DtoGeneratorOutput.BuilderMethod builderMethod(SimpleStaticMethodGoalContext goal) {
    return new DtoGeneratorOutput.BuilderMethod(
        goal.details.name,
        methodBuilder(goal.details.name + "Builder")
            .addModifiers(goal.details.access(STATIC))
            .build());
  }

  @Override
  protected ModuleOutput process(SimpleStaticMethodGoalContext goal) {
    GenericsGenerator generator = GenericsGenerator.create(goal);
    return new ModuleOutput(
        builderMethod(goal),
        asList(generator.defineImpl(), generator.defineContract()),
        emptyList());
  }
}
