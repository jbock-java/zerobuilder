package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoConstructorGoal.SimpleConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoMethodGoal.InstanceMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static net.zerobuilder.compiler.generate.ZeroUtil.applyRanking;
import static net.zerobuilder.compiler.generate.ZeroUtil.asPredicate;
import static net.zerobuilder.compiler.generate.ZeroUtil.createRanking;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;

public final class DtoRegularGoal {

  public static abstract class SimpleRegularGoalContext {

    SimpleRegularGoalContext(SimpleRegularGoalDescription description, int[] ranking) {
      this.description = description;
      this.ranking = ranking;
    }

    private final SimpleRegularGoalDescription description;

    public final SimpleRegularGoalDescription description() {
      return description;
    }

    public abstract <R> R acceptRegular(RegularGoalContextCases<R> cases);
    private final int[] ranking;
    public abstract TypeName type();

    public final GoalContext context() {
      return context.apply(this);
    }

    public final boolean isInstance() {
      return isInstance.test(this);
    }

    static int[] createUnshuffle(List<SimpleParameter> parameters, List<String> parameterNames) {
      String[] a = new String[parameters.size()];
      for (int i = 0; i < parameters.size(); i++) {
        a[i] = parameters.get(i).name;
      }
      String[] b = parameterNames.toArray(new String[parameterNames.size()]);
      return createRanking(a, b);
    }

    public <E> List<E> unshuffle(List<E> shuffled) {
      return applyRanking(ranking, shuffled);
    }

    public final CodeBlock invocationParameters() {
      List<SimpleParameter> unshuffled = unshuffle(description.parameters());
      return unshuffled.stream()
          .map(parameter -> parameter.name)
          .map(CodeBlock::of)
          .collect(joinCodeBlocks(", "));
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
      Function<SimpleConstructorGoalContext, R> constructor,
      Function<InstanceMethodGoalContext, R> instanceMethod,
      Function<SimpleStaticMethodGoalContext, R> staticMethod) {
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
