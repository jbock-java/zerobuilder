package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoConstructorGoal.SimpleConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.DescriptionInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.AbstractGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.BeanGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.ProjectedGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.RegularSimpleGoalInput;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.InstanceMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ProjectableDetailsCases;
import net.zerobuilder.compiler.generate.DtoGoalDetails.RegularGoalDetailsCases;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoMethodGoal.InstanceMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import java.util.function.Function;

import static net.zerobuilder.compiler.generate.DtoDescriptionInput.descriptionInputCases;

final class GoalContextFactory {

  private static BeanGoalContext prepareBean(
      GoalContext context,
      BeanGoalDescription description) {
    return new BeanGoalContext(context, description.details, description);
  }

  private static SimpleRegularGoalContext prepareRegular(
      GoalContext context,
      SimpleRegularGoalDescription simple) {
    return simple.details().accept(new RegularGoalDetailsCases<SimpleRegularGoalContext, Void>() {
      @Override
      public SimpleRegularGoalContext method(InstanceMethodGoalDetails details, Void _null) {
        return new InstanceMethodGoalContext(context, details, simple);
      }
      @Override
      public SimpleRegularGoalContext staticMethod(StaticMethodGoalDetails details, Void _null) {
        return new SimpleStaticMethodGoalContext(context, details, simple);
      }
      @Override
      public SimpleRegularGoalContext constructor(ConstructorGoalDetails details, Void _null) {
        return new SimpleConstructorGoalContext(context, details, simple);
      }
    }, null);
  }

  private static ProjectedRegularGoalContext prepareProjectedRegular(
      DtoContext.GoalContext context,
      ProjectedRegularGoalDescription description) {
    return description.details().accept(new ProjectableDetailsCases<ProjectedRegularGoalContext>() {
      @Override
      public ProjectedRegularGoalContext constructor(ConstructorGoalDetails constructor) {
        return new ProjectedConstructorGoalContext(context, constructor, description);
      }
      @Override
      public ProjectedRegularGoalContext method(StaticMethodGoalDetails method) {
        return new ProjectedMethodGoalContext(context, method, description);
      }
    });
  }

  static Function<DescriptionInput, AbstractGoalInput> prepare(DtoContext.GoalContext context) {
    return descriptionInputCases(
        (module, description) -> new RegularSimpleGoalInput(
            module,
            GoalContextFactory.prepareRegular(
                context, description)),
        (module, description) -> new ProjectedGoalInput(
            module,
            prepareProjectedRegular(
                context, description)),
        (module, bean) -> new BeanGoalInput(
            module, GoalContextFactory.prepareBean(
            context, bean)));
  }
}
