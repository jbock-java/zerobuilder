package net.zerobuilder.compiler.generate;

import com.google.common.collect.ImmutableList;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;

import java.util.List;

public final class GeneratorInput {

  public final ImmutableList<? extends GoalDescription> validGoals;
  public final DtoBuilders.BuildersContext buildersContext;

  private GeneratorInput(DtoBuilders.BuildersContext buildersContext, ImmutableList<? extends GoalDescription> validGoals) {
    this.validGoals = validGoals;
    this.buildersContext = buildersContext;
  }
  public static GeneratorInput create(DtoBuilders.BuildersContext buildersContext, List<? extends GoalDescription> validGoals) {
    return new GeneratorInput(buildersContext, ImmutableList.copyOf(validGoals));
  }
}
