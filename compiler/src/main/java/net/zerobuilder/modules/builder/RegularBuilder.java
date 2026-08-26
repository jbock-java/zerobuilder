package net.zerobuilder.modules.builder;

import net.zerobuilder.compiler.generate.GoalDescription;
import net.zerobuilder.compiler.generate.ModuleOutput;

public final class RegularBuilder {

  public ModuleOutput process(GoalDescription description) {
    return BuilderComponent_Impl.factory()
        .create(description)
        .createFactory()
        .process();
  }
}
