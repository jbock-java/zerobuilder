package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoModule.Module;
import net.zerobuilder.compiler.generate.DtoModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoModule.RegularContractModule;
import net.zerobuilder.compiler.generate.DtoProjectedDescription.ProjectedDescription;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleStaticGoalDescription;
import net.zerobuilder.compiler.generate.DtoSimpleDescription.SimpleDescription;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class DtoDescriptionInput {

  interface DescriptionInputCases<R> {
    R simple(SimpleDescriptionInput simple);
    R simpleRegular(SimpleRegularDescriptionInput simple);
    R projected(ProjectedDescriptionInput projected);
  }

  public interface DescriptionInput {
    <R> R accept(DescriptionInputCases<R> cases);
  }

  static <R> Function<DescriptionInput, R> asFunction(DescriptionInputCases<R> cases) {
    return descriptionInput -> descriptionInput.accept(cases);
  }

  static <R> Function<DescriptionInput, R> descriptionInputCases(
      BiFunction<Module, SimpleDescription, R> simpleFunction,
      BiFunction<RegularContractModule, SimpleStaticGoalDescription, R> simpleRegularFunction,
      BiFunction<ProjectedModule, ProjectedDescription, R> projectedFunction) {
    return asFunction(new DescriptionInputCases<R>() {
      @Override
      public R simple(SimpleDescriptionInput simple) {
        return simpleFunction.apply(simple.module, simple.description);
      }
      @Override
      public R simpleRegular(SimpleRegularDescriptionInput simple) {
        return simpleRegularFunction.apply(simple.module, simple.description);
      }
      @Override
      public R projected(ProjectedDescriptionInput projected) {
        return projectedFunction.apply(projected.module, projected.description);
      }
    });
  }

  public static final class SimpleDescriptionInput implements DescriptionInput {
    final Module module;
    final SimpleDescription description;
    public SimpleDescriptionInput(Module module, SimpleDescription description) {
      this.module = module;
      this.description = description;
    }

    @Override
    public <R> R accept(DescriptionInputCases<R> cases) {
      return cases.simple(this);
    }
  }

  public static final class SimpleRegularDescriptionInput implements DescriptionInput {
    final RegularContractModule module;
    final SimpleStaticGoalDescription description;
    public SimpleRegularDescriptionInput(RegularContractModule module, SimpleStaticGoalDescription description) {
      this.module = module;
      this.description = description;
    }

    @Override
    public <R> R accept(DescriptionInputCases<R> cases) {
      return cases.simpleRegular(this);
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
