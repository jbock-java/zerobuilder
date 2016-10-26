package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoal.RegularGoalContextCases;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;
import net.zerobuilder.compiler.generate.DtoRegularStep.SimpleRegularStep;

import java.util.List;

import static java.util.Collections.unmodifiableList;

public final class DtoConstructorGoal {

  static final class SimpleConstructorGoalContext
      extends DtoRegularGoal.AbstractRegularGoalContext {

    final List<SimpleRegularStep> steps;

    final BuildersContext context;
    final ConstructorGoalDetails details;
    final List<TypeName> thrownTypes;

    SimpleConstructorGoalContext(BuildersContext context,
                                 ConstructorGoalDetails details,
                                 List<SimpleRegularStep> steps,
                                 List<TypeName> thrownTypes) {
      this.context = context;
      this.details = details;
      this.steps = steps;
      this.thrownTypes = thrownTypes;
    }

    final List<AbstractRegularStep> constructorSteps() {
      return unmodifiableList(steps);
    }

    @Override
    public final <R> R acceptRegular(RegularGoalContextCases<R> cases) {
      return cases.constructorGoal(this);
    }

    @Override
    public final List<String> parameterNames() {
      return details.parameterNames;
    }

    @Override
    public final TypeName type() {
      return details.goalType;
    }
  }

  private DtoConstructorGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
