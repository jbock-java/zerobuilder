package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoGoalDescription.GoalDescription;
import net.zerobuilder.compiler.generate.DtoModule.Module;
import net.zerobuilder.compiler.generate.DtoProjectedDescription.ProjectedDescription;
import net.zerobuilder.compiler.generate.DtoProjectedModule.ProjectedModule;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class DtoDescriptionInput {

  interface DescriptionInputCases<R> {
    R simple(SimpleDescriptionInput simple);
    R projected(ProjectedDescriptionInput projected);
  }

  public interface DescriptionInput {
    <R> R accept(DescriptionInputCases<R> cases);
  }

  static <R> Function<DescriptionInput, R> asFunction(DescriptionInputCases<R> cases) {
    return descriptionInput -> descriptionInput.accept(cases);
  }

  static <R> Function<DescriptionInput, R> descriptionInputCases(
      BiFunction<Module, GoalDescription, R> simpleFunction,
      BiFunction<ProjectedModule, ProjectedDescription, R> projectedFunction) {
    return asFunction(new DescriptionInputCases<R>() {
      @Override
      public R simple(SimpleDescriptionInput simple) {
        return simpleFunction.apply(simple.module, simple.description);
      }
      @Override
      public R projected(ProjectedDescriptionInput projected) {
        return projectedFunction.apply(projected.module, projected.description);
      }
    });
  }

  public static final class SimpleDescriptionInput implements DescriptionInput {
    final Module module;
    final GoalDescription description;
    public SimpleDescriptionInput(Module module, GoalDescription description) {
      this.module = module;
      this.description = description;
    }

    @Override
    public <R> R accept(DescriptionInputCases<R> cases) {
      return cases.simple(this);
    }
  }

  public static final class ProjectedDescriptionInput implements DescriptionInput {
    final ProjectedModule module;
    final ProjectedDescription description;
    public ProjectedDescriptionInput(ProjectedModule module, ProjectedDescription description) {
      this.module = module;
      this.description = description;
    }

    @Override
    public <R> R accept(DescriptionInputCases<R> cases) {
      return cases.projected(this);
    }
  }

  private DtoDescriptionInput() {
    throw new UnsupportedOperationException("no instances");
  }
}
