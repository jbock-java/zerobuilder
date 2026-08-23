package net.zerobuilder.modules.builder;

import net.zerobuilder.compiler.generate.DtoModule.BuilderModule;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;
import net.zerobuilder.compiler.generate.ModuleOutput;

public final class RegularBuilder implements BuilderModule {

  @Override
  public ModuleOutput process(BuilderGoalDescription description) {
    return BuilderComponent_Impl.factory()
        .create(description)
        .createFactory()
        .process();
  }
}
