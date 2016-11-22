package net.zerobuilder.modules.generics;

import net.zerobuilder.compiler.generate.DtoModule.RegularSimpleModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public final class GenericsBuilder implements RegularSimpleModule {

  @Override
  public ModuleOutput process(SimpleRegularGoalContext goal) {
    GenericsGenerator generator = GenericsGenerator.create(goal);
    return new ModuleOutput(
        generator.builderMethod(goal),
        asList(generator.defineImpl(), generator.defineContract()),
        emptyList());
  }
}
