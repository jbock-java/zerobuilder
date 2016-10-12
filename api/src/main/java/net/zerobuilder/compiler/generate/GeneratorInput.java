package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;

import java.util.List;

import static java.util.Collections.unmodifiableList;

public final class GeneratorInput {

  public final List<GoalDescription> validGoals;
  public final DtoContext.BuildersContext buildersContext;

  private GeneratorInput(DtoContext.BuildersContext buildersContext, List<GoalDescription> validGoals) {
    this.validGoals = validGoals;
    this.buildersContext = buildersContext;
  }

  public static GeneratorInput create(DtoContext.BuildersContext buildersContext, List<? extends GoalDescription> goalDescriptions) {
    return new GeneratorInput(buildersContext, unmodifiableList(goalDescriptions));
  }
}
