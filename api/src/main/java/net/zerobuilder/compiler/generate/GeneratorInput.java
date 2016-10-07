package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;

import java.util.List;

import static net.zerobuilder.compiler.generate.Utilities.generalize;

public final class GeneratorInput {

  public final List<GoalDescription> validGoals;
  public final DtoBuildersContext.BuildersContext buildersContext;

  private GeneratorInput(DtoBuildersContext.BuildersContext buildersContext, List<GoalDescription> validGoals) {
    this.validGoals = validGoals;
    this.buildersContext = buildersContext;
  }

  public static GeneratorInput create(DtoBuildersContext.BuildersContext buildersContext, List<? extends GoalDescription> goalDescriptions) {
    return new GeneratorInput(buildersContext, generalize(goalDescriptions));
  }
}
