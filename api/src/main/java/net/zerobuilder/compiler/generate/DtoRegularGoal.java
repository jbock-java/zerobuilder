package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoConstructorGoal.SimpleConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoMethodGoal.InstanceMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Optional.empty;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.ZeroUtil.applyRanking;
import static net.zerobuilder.compiler.generate.ZeroUtil.asPredicate;
import static net.zerobuilder.compiler.generate.ZeroUtil.createRanking;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;

public final class DtoRegularGoal {

  public static abstract class SimpleRegularGoalContext
      extends RegularGoalContext implements SimpleGoal {

    SimpleRegularGoalContext(SimpleRegularGoalDescription description, int[] ranking) {
      super(description.thrownTypes);
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

    public final AbstractRegularDetails regularDetails() {
      return goalDetails.apply(this);
    }

    public final boolean isInstance() {
      return isInstance.test(this);
    }

    public final Optional<FieldSpec> maybeField() {
      return maybeField.apply(this);
    }

    @Override
    public final <R> R acceptRegular(DtoRegularGoalContext.RegularGoalContextCases<R> cases) {
      return cases.simple(this);
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

    @Override
    public final <R> R acceptSimple(DtoSimpleGoal.SimpleGoalCases<R> cases) {
      return cases.regular(this);
    }

    public final CodeBlock invocationParameters() {
      List<SimpleParameter> unshuffled = unshuffle(description.parameters());
      return unshuffled.stream()
          .map(parameter -> parameter.name)
          .map(CodeBlock::of)
          .collect(joinCodeBlocks(", "));
    }
  }

  public static final Function<SimpleRegularGoalContext, AbstractRegularDetails> goalDetails =
      regularGoalContextCases(
          constructor -> constructor.details,
          method -> method.details,
          staticMethod -> staticMethod.details);

  public static final Function<SimpleRegularGoalContext, Boolean> mayReuse =
      regularGoalContextCases(
          constructor -> constructor.context.lifecycle == REUSE_INSTANCES
              && constructor.details.instanceTypeParameters.isEmpty()
              && constructor.details.instanceTypeParameters.isEmpty(),
          method -> method.context.lifecycle == REUSE_INSTANCES,
          staticMethod -> staticMethod.context.lifecycle == REUSE_INSTANCES
              && staticMethod.details.typeParameters.isEmpty());

  public static final Predicate<SimpleRegularGoalContext> isInstance =
      asPredicate(regularGoalContextCases(
          constructor -> false,
          instanceMethod -> true,
          staticMethod -> false));

  public static final Function<SimpleRegularGoalContext, List<TypeName>> stepTypes =
      regularGoalContextCases(
          constructor -> transform(constructor.description().parameters(), step -> step.type),
          instanceMethod -> transform(instanceMethod.description().parameters(), step -> step.type),
          staticMethod -> transform(staticMethod.description().parameters(), step -> step.type));

  public static final Function<SimpleRegularGoalContext, Optional<FieldSpec>> maybeField =
      regularGoalContextCases(
          constructor -> empty(),
          method -> Optional.of(method.instanceField()),
          staticMethod -> empty());

  public interface RegularGoalContextCases<R> {
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
