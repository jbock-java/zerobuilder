package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;

import java.util.List;

public class DtoIConstructorGoal {

  static final class ConstructorGoal implements DtoGoalContext.IGoal {
    final DtoGoal.ConstructorGoalDetails details;

    final List<DtoRegularStep.AbstractRegularStep> steps;
    final List<TypeName> thrownTypes;

    private ConstructorGoal(DtoGoal.ConstructorGoalDetails details,
                            List<DtoRegularStep.AbstractRegularStep> steps,
                            List<TypeName> thrownTypes) {
      this.steps = steps;
      this.thrownTypes = thrownTypes;
      this.details = details;
    }

    static ConstructorGoal create(DtoGoal.ConstructorGoalDetails details,
                                  List<DtoRegularStep.AbstractRegularStep> steps,
                                  List<TypeName> thrownTypes) {
      return new ConstructorGoal(details, steps, thrownTypes);
    }

    @Override
    public DtoGoalContext.AbstractGoalContext withContext(DtoContext.BuildersContext context) {
      return new DtoConstructorGoal.ConstructorGoalContext(this, context);
    }
  }

  private DtoIConstructorGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
