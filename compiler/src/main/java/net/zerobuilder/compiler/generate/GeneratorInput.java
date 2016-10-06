package net.zerobuilder.compiler.generate;

import com.google.common.collect.ImmutableList;
import net.zerobuilder.compiler.generate.DtoValidGoal.ValidGoal;

import java.util.List;

public final class GeneratorInput {

  public final ImmutableList<? extends ValidGoal> validGoals;
  public final DtoBuilders.BuildersContext buildersContext;

  private GeneratorInput(DtoBuilders.BuildersContext buildersContext, ImmutableList<? extends ValidGoal> validGoals) {
    this.validGoals = validGoals;
    this.buildersContext = buildersContext;
  }
  public static GeneratorInput create(DtoBuilders.BuildersContext buildersContext, List<? extends ValidGoal> validGoals) {
    return new GeneratorInput(buildersContext, ImmutableList.copyOf(validGoals));
  }
}
