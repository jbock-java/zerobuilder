package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.DescriptionInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.AbstractGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.BeanGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.ProjectedGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.RegularSimpleGoalInput;

import java.util.function.Function;

import static net.zerobuilder.compiler.generate.DtoDescriptionInput.descriptionInputCases;

final class GoalContextFactory {

  private static BeanGoalContext prepareBean(
      BeanGoalDescription description) {
    return new BeanGoalContext(description.details, description);
  }

  static final Function<DescriptionInput, AbstractGoalInput> prepare =
      descriptionInputCases(
          RegularSimpleGoalInput::new,
          ProjectedGoalInput::new,
          (module, bean) -> new BeanGoalInput(
              module, prepareBean(bean)));

}
