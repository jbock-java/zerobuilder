package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;
import net.zerobuilder.compiler.generate.DtoModule.Module;

import java.util.List;

public final class DtoGeneratorInput {

  public static final class DescriptionInput {
    final Module module;
    final GoalDescription description;
    public DescriptionInput(Module module, GoalDescription description) {
      this.module = module;
      this.description = description;
    }
  }

  static final class AbstractGoalInput {
    final Module module;
    final AbstractGoalContext goal;
    AbstractGoalInput(Module module, AbstractGoalContext goal) {
      this.module = module;
      this.goal = goal;
    }
  }

  public static final class GeneratorInput {
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

  private DtoGeneratorInput() {
    throw new UnsupportedOperationException("no instances");
  }
}
