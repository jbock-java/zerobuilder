package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoModule.Module;
import net.zerobuilder.compiler.generate.DtoModule.RegularContractModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.AbstractModuleOutput;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;
import net.zerobuilder.compiler.generate.DtoProjectedModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;

import java.util.Optional;
import java.util.function.Function;

class DtoInputOutput {

  interface InputOutputCases<R> {
    R simple(InputOutput simple);
    R projected(ProjectedInputOutput projected);
    R simpleRegular(SimpleRegularInputOutput simpleRegular);
  }

  static <R> Function<AbstractInputOutput, R> asFunction(InputOutputCases<R> cases) {
    return inputOutput -> inputOutput.accept(cases);
  }

  static <R> Function<AbstractInputOutput, R> inputOutputCases(
      Function<InputOutput, R> simpleFunction,
      Function<ProjectedInputOutput, R> projectedFunction,
      Function<SimpleRegularInputOutput, R> simpleRegularFunction) {
    return asFunction(new InputOutputCases<R>() {
      @Override
      public R simple(InputOutput simple) {
        return simpleFunction.apply(simple);
      }
      @Override
      public R projected(ProjectedInputOutput projected) {
        return projectedFunction.apply(projected);
      }
      @Override
      public R simpleRegular(SimpleRegularInputOutput simpleRegular) {
        return simpleRegularFunction.apply(simpleRegular);
      }
    });
  }

  static abstract class AbstractInputOutput {
    final AbstractModuleOutput output;
    final Optional<FieldSpec> cacheField() {
      return cacheField.apply(this);
    }

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

  private static Function<AbstractInputOutput, Optional<FieldSpec>> cacheField =
      inputOutputCases(
          simple -> Optional.of(simple.module.cacheField(simple.goal)),
          projected -> Optional.of(projected.module.cacheField(projected.goal)),
          simpleRegular -> Optional.empty());

  private DtoInputOutput() {
    throw new UnsupportedOperationException("no instances");
  }
}
