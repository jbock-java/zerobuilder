package net.zerobuilder.modules.generics;

import net.zerobuilder.compiler.generate.DtoModule.RegularSimpleModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public final class GenericsBuilder implements RegularSimpleModule {

  @Override
  public ModuleOutput process(SimpleRegularGoalDescription description) {
    GenericsGenerator generator = GenericsGenerator.create(description);
    return new ModuleOutput(
        generator.builderMethod(description),
        asList(generator.defineImpl(), generator.defineContract()),
        emptyList());
  }
}
