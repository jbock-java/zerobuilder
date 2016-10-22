package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoConstructorGoal.AbstractConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractRegularGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoMethodGoal.MethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static java.util.Optional.empty;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoGoal.GoalMethodType.INSTANCE_METHOD;
import static net.zerobuilder.compiler.generate.Utilities.asPredicate;
import static net.zerobuilder.compiler.generate.Utilities.constructor;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;

final class DtoRegularGoal {

  static abstract class AbstractRegularGoalContext extends AbstractGoalContext {

    abstract <R> R acceptRegular(RegularGoalContextCases<R> cases);
    abstract List<String> parameterNames();
    abstract TypeName type();

    final AbstractRegularGoalDetails regularDetails() {
      return goalDetails.apply(this);
    }

    final boolean isInstance() {
      return isInstance.test(this);
    }

    final BuildersContext context() {
      return context.apply(this);
    }

    final List<AbstractRegularStep> regularSteps() {
      return regularSteps.apply(this);
    }

    final Optional<FieldSpec> fields() {
      return fields.apply(this);
    }

    final MethodSpec builderConstructor() {
      return builderConstructor.apply(this);
    }
  }

  private static final Function<AbstractRegularGoalContext, AbstractRegularGoalDetails> goalDetails =
      regularGoalContextCases(
          constructor -> constructor.details,
          method -> method.details);

  private static final Predicate<AbstractRegularGoalContext> isInstance =
      asPredicate(regularGoalContextCases(
          constructor -> false,
          method -> method.details.methodType == INSTANCE_METHOD));

  private static final Function<AbstractRegularGoalContext, BuildersContext> context =
      regularGoalContextCases(
          constructor -> constructor.context,
          method -> method.context);

  private static final Function<AbstractRegularGoalContext, List<AbstractRegularStep>> regularSteps =
      regularGoalContextCases(
          constructor -> constructor.constructorSteps(),
          method -> method.steps);

  private static final Function<AbstractRegularGoalContext, Optional<FieldSpec>> fields =
      regularGoalContextCases(
          constructor -> empty(),
          method -> isInstance.test(method) ?
              Optional.of(method.field()) :
              empty());

  private static final Function<AbstractRegularGoalContext, MethodSpec> builderConstructor =
      regularGoalContextCases(
          cGoal -> constructor(PRIVATE),
          mGoal -> {
            if (!isInstance.test(mGoal)
                || mGoal.context.lifecycle == REUSE_INSTANCES) {
              return constructor(PRIVATE);
            }
            ClassName type = mGoal.context.type;
            ParameterSpec parameter = parameterSpec(type, downcase(type.simpleName()));
            return constructorBuilder()
                .addParameter(parameter)
                .addStatement("this.$N = $N", mGoal.field(), parameter)
                .addModifiers(PRIVATE)
                .build();
          });

  interface RegularGoalContextCases<R> {
    R constructorGoal(AbstractConstructorGoalContext goal);
    R methodGoal(MethodGoalContext goal);
  }

  static <R> Function<AbstractRegularGoalContext, R> asFunction(RegularGoalContextCases<R> cases) {
    return goal -> goal.acceptRegular(cases);
  }

  static <R> Function<AbstractRegularGoalContext, R> regularGoalContextCases(
      Function<AbstractConstructorGoalContext, R> constructor,
      Function<MethodGoalContext, R> method) {
    return asFunction(new RegularGoalContextCases<R>() {
      @Override
      public R constructorGoal(AbstractConstructorGoalContext goal) {
        return constructor.apply(goal);
      }
      @Override
      public R methodGoal(MethodGoalContext goal) {
        return method.apply(goal);
      }
    });
  }

  private DtoRegularGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
