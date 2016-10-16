package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;

import java.util.List;

import static java.util.Collections.unmodifiableList;

public final class GeneratorInput {

  public final List<GoalDescription> goals;
  public final BuildersContext context;

  private GeneratorInput(BuildersContext context, List<GoalDescription> goals) {
    this.goals = goals;
    this.context = context;
  }

  public static GeneratorInput create(BuildersContext buildersContext, List<? extends GoalDescription> goals) {
    return new GeneratorInput(buildersContext, unmodifiableList(goals));
  }
}
