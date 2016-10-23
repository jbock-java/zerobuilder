package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;

import java.util.List;

public final class GeneratorInput {

  public static final class DescriptionInput {
    final DtoModule.Module module;
    final GoalDescription description;
    public DescriptionInput(DtoModule.Module module, GoalDescription description) {
      this.module = module;
      this.description = description;
    }
  }

  static final class AbstractGoalInput {
    final DtoModule.Module module;
    final DtoGoalContext.AbstractGoalContext goal;
    public AbstractGoalInput(DtoModule.Module module, DtoGoalContext.AbstractGoalContext goal) {
      this.module = module;
      this.goal = goal;
    }
  }

  public final List<DescriptionInput> goals;
  public final BuildersContext context;

  private GeneratorInput(BuildersContext context, List<DescriptionInput> goals) {
    this.goals = goals;
    this.context = context;
  }

  public static GeneratorInput create(BuildersContext context, List<DescriptionInput> goals) {
    return new GeneratorInput(context, goals);
  }
}
