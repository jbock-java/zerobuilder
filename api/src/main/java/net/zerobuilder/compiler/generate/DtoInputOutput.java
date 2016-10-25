package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import net.zerobuilder.compiler.generate.DtoModuleOutput.AbstractModuleOutput;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;
import net.zerobuilder.compiler.generate.DtoProjectedModule.ProjectedModule;

import java.util.function.Function;

class DtoInputOutput {

  interface InputOutputCases<R> {
    R simple(InputOutput simple);
    R projected(ProjectedInputOutput projected);
  }

  static <R> Function<AbstractInputOutput, R> asFunction(InputOutputCases<R> cases) {
    return inputOutput -> inputOutput.accept(cases);
  }

  static <R> Function<AbstractInputOutput, R> inputOutputCases(
      Function<InputOutput, R> simpleFunction,
      Function<ProjectedInputOutput, R> projectedFunction) {
    return asFunction(new InputOutputCases<R>() {
      @Override
      public R simple(InputOutput simple) {
        return simpleFunction.apply(simple);
      }
      @Override
      public R projected(ProjectedInputOutput projected) {
        return projectedFunction.apply(projected);
      }
    });
  }

  static abstract class AbstractInputOutput {
    final AbstractModuleOutput output;
    final FieldSpec cacheField() {
      return cacheField.apply(this);
    }

    AbstractInputOutput(AbstractModuleOutput output) {
      this.output = output;
    }

    abstract <R> R accept(InputOutputCases<R> cases);
  }

  static final class InputOutput extends AbstractInputOutput {
    private final DtoModule.Module module;
    private final DtoGoalContext.AbstractGoalContext goal;

    InputOutput(DtoModule.Module module, DtoGoalContext.AbstractGoalContext goal, AbstractModuleOutput output) {
      super(output);
      this.module = module;
      this.goal = goal;
    }

    @Override
    <R> R accept(InputOutputCases<R> cases) {
      return cases.simple(this);
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

  private static Function<AbstractInputOutput, FieldSpec> cacheField =
      inputOutputCases(
          simple -> simple.module.cacheField(simple.goal),
          projected -> projected.module.cacheField(projected.goal));

  private DtoInputOutput() {
    throw new UnsupportedOperationException("no instances");
  }
}
