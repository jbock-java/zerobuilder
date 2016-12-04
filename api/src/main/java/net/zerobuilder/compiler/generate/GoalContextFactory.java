package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoConstructorGoal.SimpleConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.DescriptionInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.AbstractGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.BeanGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.ProjectedGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.RegularSimpleGoalInput;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.InstanceMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.RegularGoalDetailsCases;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoMethodGoal.InstanceMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedInstanceMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import java.util.function.Function;

import static net.zerobuilder.compiler.generate.DtoDescriptionInput.descriptionInputCases;

final class GoalContextFactory {

  private static BeanGoalContext prepareBean(
      BeanGoalDescription description) {
    return new BeanGoalContext(description.details, description);
  }

  private static SimpleRegularGoalContext prepareRegular(
      SimpleRegularGoalDescription simple) {
    return simple.details.accept(new RegularGoalDetailsCases<SimpleRegularGoalContext, Void>() {
      @Override
      public SimpleRegularGoalContext method(InstanceMethodGoalDetails details, Void _null) {
        return new InstanceMethodGoalContext(simple);
      }
      @Override
      public SimpleRegularGoalContext staticMethod(StaticMethodGoalDetails details, Void _null) {
        return new SimpleStaticMethodGoalContext(simple);
      }
      @Override
      public SimpleRegularGoalContext constructor(ConstructorGoalDetails details, Void _null) {
        return new SimpleConstructorGoalContext(simple);
      }
    }, null);
  }

  private static ProjectedRegularGoalContext prepareProjectedRegular(
      ProjectedRegularGoalDescription description) {
    return description.details.accept(new RegularGoalDetailsCases<ProjectedRegularGoalContext, Void>() {
      @Override
      public ProjectedRegularGoalContext method(InstanceMethodGoalDetails details, Void _null) {
        return new ProjectedInstanceMethodGoalContext(details, description);
      }
      @Override
      public ProjectedRegularGoalContext staticMethod(StaticMethodGoalDetails method, Void _null) {
        return new ProjectedMethodGoalContext(method, description);
      }
      @Override
      public ProjectedRegularGoalContext constructor(ConstructorGoalDetails constructor, Void _null) {
        return new ProjectedConstructorGoalContext(constructor, description);
      }
    }, null);
  }

  static final Function<DescriptionInput, AbstractGoalInput> prepare =
      descriptionInputCases(
          (module, description) -> new RegularSimpleGoalInput(
              module, prepareRegular(description)),
          (module, description) -> new ProjectedGoalInput(
              module, prepareProjectedRegular(description)),
          (module, bean) -> new BeanGoalInput(
              module, prepareBean(bean)));

}
