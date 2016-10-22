package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoMethodGoal.MethodGoalContext;

import java.util.List;

public class DtoIMethodGoal {

  static final class MethodGoal implements DtoGoalContext.IGoal {
    final DtoGoal.MethodGoalDetails details;

    final List<DtoRegularStep.AbstractRegularStep> steps;
    final List<TypeName> thrownTypes;

    private MethodGoal(DtoGoal.MethodGoalDetails details,
                       List<DtoRegularStep.AbstractRegularStep> steps,
                       List<TypeName> thrownTypes) {
      this.steps = steps;
      this.thrownTypes = thrownTypes;
      this.details = details;
    }

    static MethodGoal create(DtoGoal.MethodGoalDetails details,
                             List<DtoRegularStep.AbstractRegularStep> steps,
                             List<TypeName> thrownTypes) {
      return new MethodGoal(details, steps, thrownTypes);
    }

    @Override
    public DtoGoalContext.AbstractGoalContext withContext(DtoContext.BuildersContext context) {
      return new MethodGoalContext(this, context);
    }
  }

  private DtoIMethodGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
