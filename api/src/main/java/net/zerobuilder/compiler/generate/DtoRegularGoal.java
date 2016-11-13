package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoConstructorGoal.SimpleConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoMethodGoal.InstanceMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Optional.empty;
import static net.zerobuilder.compiler.generate.ZeroUtil.asPredicate;

public final class DtoRegularGoal {

  public static abstract class SimpleRegularGoalContext
      extends RegularGoalContext implements SimpleGoal {

    SimpleRegularGoalContext(List<TypeName> thrownTypes) {
      super(thrownTypes);
    }

    public abstract <R> R acceptRegular(RegularGoalContextCases<R> cases);
    abstract List<String> parameterNames();
    abstract TypeName type();

    public final AbstractRegularDetails regularDetails() {
      return goalDetails.apply(this);
    }

    public final boolean isInstance() {
      return isInstance.test(this);
    }

    public final List<? extends AbstractRegularStep> regularSteps() {
      return regularSteps.apply(this);
    }

    public final Optional<FieldSpec> maybeField() {
      return maybeField.apply(this);
    }

    @Override
    final <R> R acceptRegular(DtoRegularGoalContext.RegularGoalContextCases<R> cases) {
      return cases.simple(this);
    }

    @Override
    public final <R> R acceptSimple(DtoSimpleGoal.SimpleGoalCases<R> cases) {
      return cases.regular(this);
    }

    public final CodeBlock invocationParameters() {
      return CodeBlock.of(String.join(", ", parameterNames()));
    }
  }

  static final Function<SimpleRegularGoalContext, AbstractRegularDetails> goalDetails =
      regularGoalContextCases(
          constructor -> constructor.details,
          method -> method.details,
          staticMethod -> staticMethod.details);

  private static final Predicate<SimpleRegularGoalContext> isInstance =
      asPredicate(regularGoalContextCases(
          constructor -> false,
          instance -> true,
          simple -> simple.details.instance));

  private static final Function<SimpleRegularGoalContext, List<? extends AbstractRegularStep>> regularSteps =
      regularGoalContextCases(
          constructor -> constructor.steps,
          method -> method.steps,
          staticMethod -> staticMethod.steps);

  private static final Function<SimpleRegularGoalContext, Optional<FieldSpec>> maybeField =
      regularGoalContextCases(
          constructor -> empty(),
          method -> Optional.of(method.instanceField()),
          staticMethod -> empty());

  public interface RegularGoalContextCases<R> {
    R constructor(SimpleConstructorGoalContext goal);
    R instanceMethod(InstanceMethodGoalContext goal);
    R staticMethod(SimpleStaticMethodGoalContext goal);
  }

  static <R> Function<SimpleRegularGoalContext, R> asFunction(RegularGoalContextCases<R> cases) {
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
