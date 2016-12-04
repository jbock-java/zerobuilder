package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoConstructorGoal.SimpleConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoMethodGoal.InstanceMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import java.util.function.Function;

public final class DtoRegularGoal {

  public static abstract class SimpleRegularGoalContext {

    SimpleRegularGoalContext(SimpleRegularGoalDescription description) {
      this.description = description;
    }

    public final SimpleRegularGoalDescription description;

    public final SimpleRegularGoalDescription description() {
      return description;
    }

    public abstract <R> R acceptRegular(RegularGoalContextCases<R> cases);
  }

  interface RegularGoalContextCases<R> {
    R constructor(SimpleConstructorGoalContext goal);
    R instanceMethod(InstanceMethodGoalContext goal);
    R staticMethod(SimpleStaticMethodGoalContext goal);
  }

  private static <R> Function<SimpleRegularGoalContext, R> asFunction(RegularGoalContextCases<R> cases) {
    return goal -> goal.acceptRegular(cases);
  }

  public static <R> Function<SimpleRegularGoalContext, R> regularGoalContextCases(
      Function<? super SimpleConstructorGoalContext, ? extends R> constructor,
      Function<? super InstanceMethodGoalContext, ? extends R> instanceMethod,
      Function<? super SimpleStaticMethodGoalContext, ? extends R> staticMethod) {
    return asFunction(new RegularGoalContextCases<R>() {
      @Override
      public R constructor(SimpleConstructorGoalContext goal) {
        return constructor.apply(goal);
      }
      @Override
      public R instanceMethod(InstanceMethodGoalContext goal) {
        return instanceMethod.apply(goal);
      }
      @Override
      public R staticMethod(SimpleStaticMethodGoalContext goal) {
        return staticMethod.apply(goal);
      }
    });
  }

  private DtoRegularGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
