package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoModule.Module;
import net.zerobuilder.compiler.generate.DtoModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoModule.RegularContractModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.AbstractModuleOutput;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;

class DtoInputOutput {

  interface InputOutputCases<R> {
    R simple(InputOutput simple);
    R projected(ProjectedInputOutput projected);
    R simpleRegular(SimpleRegularInputOutput simpleRegular);
  }

  static abstract class AbstractInputOutput {
    private final AbstractModuleOutput output;
    final AbstractModuleOutput output() {
      return output;
    }

    AbstractInputOutput(AbstractModuleOutput output) {
      this.output = output;
    }

    abstract <R> R accept(InputOutputCases<R> cases);
  }

  static final class InputOutput extends AbstractInputOutput {
    private final Module module;
    private final SimpleGoal goal;

    private InputOutput(Module module, SimpleGoal goal, AbstractModuleOutput output) {
      super(output);
      this.module = module;
      this.goal = goal;
    }

    static InputOutput create(Module module, SimpleGoal goal) {
      return new InputOutput(module, goal, module.process(goal));
    }

    @Override
    <R> R accept(InputOutputCases<R> cases) {
      return cases.simple(this);
    }
  }

  static final class SimpleRegularInputOutput extends AbstractInputOutput {
    private final RegularContractModule module;
    private final SimpleStaticMethodGoalContext goal;

    private SimpleRegularInputOutput(RegularContractModule module, SimpleStaticMethodGoalContext goal, AbstractModuleOutput output) {
      super(output);
      this.module = module;
      this.goal = goal;
    }

    static SimpleRegularInputOutput create(RegularContractModule module, SimpleStaticMethodGoalContext goal) {
      return new SimpleRegularInputOutput(module, goal, module.process(goal));
    }

    @Override
    <R> R accept(InputOutputCases<R> cases) {
      return cases.simpleRegular(this);
    }
  }

  static final class ProjectedInputOutput extends AbstractInputOutput {
    private final ProjectedModule module;
    private final ProjectedGoal goal;

    private ProjectedInputOutput(ProjectedModule module, ProjectedGoal goal, AbstractModuleOutput output) {
      super(output);
      this.module = module;
      this.goal = goal;
    }

    static ProjectedInputOutput create(ProjectedModule module, ProjectedGoal goal) {
      return new ProjectedInputOutput(module, goal, module.process(goal));
    }

    @Override
    <R> R accept(InputOutputCases<R> cases) {
      return cases.projected(this);
    }
  }

  private DtoInputOutput() {
    throw new UnsupportedOperationException("no instances");
  }
}
