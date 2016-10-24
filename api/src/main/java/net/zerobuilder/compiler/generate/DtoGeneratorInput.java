package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.DescriptionInput;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoModule.Module;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;
import net.zerobuilder.compiler.generate.DtoProjectedModule.ProjectedModule;

import java.util.List;

public final class DtoGeneratorInput {

  static final class AbstractGoalInput {
    final Module module;
    final AbstractGoalContext goal;
    AbstractGoalInput(Module module, AbstractGoalContext goal) {
      this.module = module;
      this.goal = goal;
    }
  }

  static final class ProjectedGoalInput {
    final ProjectedModule module;
    final ProjectedGoal goal;
    ProjectedGoalInput(ProjectedModule module, ProjectedGoal goal) {
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
