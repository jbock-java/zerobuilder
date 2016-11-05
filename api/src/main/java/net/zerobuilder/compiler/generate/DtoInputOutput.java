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
    final AbstractModuleOutput output;

    AbstractInputOutput(AbstractModuleOutput output) {
      this.output = output;
    }

    abstract <R> R accept(InputOutputCases<R> cases);
  }

  static final class InputOutput extends AbstractInputOutput {
    private final Module module;
    private final SimpleGoal goal;

    InputOutput(Module module, SimpleGoal goal, AbstractModuleOutput output) {
      super(output);
      this.module = module;
      this.goal = goal;
    }

    @Override
    <R> R accept(InputOutputCases<R> cases) {
      return cases.simple(this);
    }
  }

  static final class SimpleRegularInputOutput extends AbstractInputOutput {
    private final RegularContractModule module;
    private final SimpleStaticMethodGoalContext goal;

    SimpleRegularInputOutput(RegularContractModule module, SimpleStaticMethodGoalContext goal, AbstractModuleOutput output) {
      super(output);
      this.module = module;
      this.goal = goal;
    }

    @Override
    <R> R accept(InputOutputCases<R> cases) {
      return cases.simpleRegular(this);
    }
  }

  static final class ProjectedInputOutput extends AbstractInputOutput {
    final ProjectedModule module;
    final ProjectedGoal goal;

    ProjectedInputOutput(ProjectedModule module, ProjectedGoal goal, AbstractModuleOutput output) {
      super(output);
      this.module = module;
      this.goal = goal;
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
