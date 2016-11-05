package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ContractModuleOutput;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import java.util.List;

import static net.zerobuilder.compiler.generate.DtoSimpleGoal.abstractSteps;

public final class DtoModule {

  public static abstract class Module {
    protected abstract String name();
    protected abstract ContractModuleOutput process(SimpleGoal goal);
    protected final List<? extends AbstractStep> steps(SimpleGoal goal) {
      return abstractSteps.apply(goal);
    }
  }

  public static abstract class RegularContractModule {
    protected abstract ContractModuleOutput process(SimpleStaticMethodGoalContext goal);
  }

  private DtoModule() {
    throw new UnsupportedOperationException("no instances");
  }
}
