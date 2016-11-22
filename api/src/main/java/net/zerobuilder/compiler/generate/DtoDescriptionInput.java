package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoModule.BeanModule;
import net.zerobuilder.compiler.generate.DtoModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoModule.RegularSimpleModule;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class DtoDescriptionInput {

  interface DescriptionInputCases<R> {
    R regularSimple(RegularSimpleDescriptionInput simple);
    R projected(ProjectedDescriptionInput projected);
    R bean(BeanDescriptionInput bean);
  }

  public interface DescriptionInput {
    <R> R accept(DescriptionInputCases<R> cases);
  }

  static <R> Function<DescriptionInput, R> asFunction(DescriptionInputCases<R> cases) {
    return descriptionInput -> descriptionInput.accept(cases);
  }

  static <R> Function<DescriptionInput, R> descriptionInputCases(
      BiFunction<RegularSimpleModule, SimpleRegularGoalDescription, R> regularSimpleFunction,
      BiFunction<ProjectedModule, ProjectedRegularGoalDescription, R> projectedFunction,
      BiFunction<BeanModule, BeanGoalDescription, R> beanFunction) {
    return asFunction(new DescriptionInputCases<R>() {
      @Override
      public R regularSimple(RegularSimpleDescriptionInput simple) {
        return regularSimpleFunction.apply(simple.module, simple.description);
      }
      @Override
      public R projected(ProjectedDescriptionInput projected) {
        return projectedFunction.apply(projected.module, projected.description);
      }
      @Override
      public R bean(BeanDescriptionInput bean) {
        return beanFunction.apply(bean.module, bean.description);
      }
    });
  }

  public static final class BeanDescriptionInput implements DescriptionInput {
    final BeanModule module;
    final BeanGoalDescription description;
    public BeanDescriptionInput(BeanModule module, BeanGoalDescription description) {
      this.module = module;
      this.description = description;
    }

    @Override
    public <R> R accept(DescriptionInputCases<R> cases) {
      return cases.bean(this);
    }
  }

  public static final class ProjectedDescriptionInput implements DescriptionInput {
    final ProjectedModule module;
    final ProjectedRegularGoalDescription description;
    public ProjectedDescriptionInput(ProjectedModule module, ProjectedRegularGoalDescription description) {
      this.module = module;
      this.description = description;
    }

    @Override
    public <R> R accept(DescriptionInputCases<R> cases) {
      return cases.projected(this);
    }
  }

  public static final class RegularSimpleDescriptionInput implements DescriptionInput {
    final RegularSimpleModule module;
    final SimpleRegularGoalDescription description;
    public RegularSimpleDescriptionInput(RegularSimpleModule module, SimpleRegularGoalDescription description) {
      this.module = module;
      this.description = description;
    }
    @Override
    public <R> R accept(DescriptionInputCases<R> cases) {
      return cases.regularSimple(this);
    }
  }

  private DtoDescriptionInput() {
    throw new UnsupportedOperationException("no instances");
  }
}
