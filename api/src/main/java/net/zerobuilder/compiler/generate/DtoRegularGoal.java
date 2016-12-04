package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoConstructorGoal.SimpleConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoMethodGoal.InstanceMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import java.util.function.Function;
import java.util.function.Predicate;

import static net.zerobuilder.compiler.generate.ZeroUtil.asPredicate;

public final class DtoRegularGoal {

  public static abstract class SimpleRegularGoalContext {

    SimpleRegularGoalContext(SimpleRegularGoalDescription description) {
      this.description = description;
    }

    private final SimpleRegularGoalDescription description;

    public final SimpleRegularGoalDescription description() {
      return description;
    }

    public abstract <R> R acceptRegular(RegularGoalContextCases<R> cases);
    public abstract TypeName type();

    public final GoalContext context() {
      return context.apply(this);
    }

    public final boolean isInstance() {
      return isInstance.test(this);
    }

  }

  private static final Predicate<SimpleRegularGoalContext> isInstance =
      asPredicate(regularGoalContextCases(
          constructor -> false,
          instanceMethod -> true,
          staticMethod -> false));

  private static final Function<SimpleRegularGoalContext, GoalContext> context =
      regularGoalContextCases(
          constructor -> constructor.context,
          instanceMethod -> instanceMethod.context,
          staticMethod -> staticMethod.context);

  interface RegularGoalContextCases<R> {
    R constructor(SimpleConstructorGoalContext goal);
    R instanceMethod(InstanceMethodGoalContext goal);
    R staticMethod(SimpleStaticMethodGoalContext goal);
  }

  public static <R> Function<SimpleRegularGoalContext, R> asFunction(RegularGoalContextCases<R> cases) {
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
