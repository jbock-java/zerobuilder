package net.zerobuilder.modules.generics;

import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public final class GenericsBuilder extends DtoModule.RegularContractModule {

  @Override
  protected ModuleOutput process(SimpleStaticMethodGoalContext goal) {
    GenericsGenerator generator = GenericsGenerator.create(goal);
    return new ModuleOutput(
        generator.builderMethod(goal),
        asList(generator.defineImpl(), generator.defineContract()),
        emptyList());
  }
}
