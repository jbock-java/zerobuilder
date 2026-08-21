package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoModule.BeanModule;
import net.zerobuilder.compiler.generate.DtoModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoModule.RegularSimpleModule;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

public final class DtoGeneratorInput {

  public sealed interface AbstractGoalInput permits ProjectedGoalInput, RegularSimpleGoalInput, BeanGoalInput {
  }

  public static final class RegularSimpleGoalInput implements AbstractGoalInput {
    final RegularSimpleModule module;
    final SimpleRegularGoalDescription description;

    public RegularSimpleGoalInput(RegularSimpleModule module, SimpleRegularGoalDescription description) {
      this.module = module;
      this.description = description;
    }
  }

  public static final class BeanGoalInput implements AbstractGoalInput {
    final BeanModule module;
    final BeanGoalDescription description;

    public BeanGoalInput(BeanModule module, BeanGoalDescription description) {
      this.module = module;
      this.description = description;
    }
  }

  public static final class ProjectedGoalInput implements AbstractGoalInput {
    final ProjectedModule module;
    final ProjectedRegularGoalDescription description;

    public ProjectedGoalInput(ProjectedModule module, ProjectedRegularGoalDescription description) {
      this.module = module;
      this.description = description;
    }
  }

  static GoalContext getContext(AbstractGoalInput goalInput) {
    return switch (goalInput) {
      case ProjectedGoalInput projected -> projected.description.context;
      case RegularSimpleGoalInput regular -> regular.description.context;
      case BeanGoalInput bean -> bean.description.details.context;
    };
  }

  private DtoGeneratorInput() {
    throw new UnsupportedOperationException("no instances");
  }
}
