package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoConstructorGoal.SimpleConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractRegularGoalDetails;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;

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

  static abstract class SimpleRegularGoalContext
      extends RegularGoalContext implements SimpleGoal {

    SimpleRegularGoalContext(List<TypeName> thrownTypes) {
      super(thrownTypes);
    }

    abstract <R> R acceptRegular(RegularGoalContextCases<R> cases);
    abstract List<String> parameterNames();
    abstract TypeName type();

    final AbstractRegularGoalDetails regularDetails() {
      return goalDetails.apply(this);
    }

    final boolean isInstance() {
      return isInstance.test(this);
    }

    final List<? extends AbstractRegularStep> regularSteps() {
      return regularSteps.apply(this);
    }

    final Optional<FieldSpec> fields() {
      return fields.apply(this);
    }

    final MethodSpec builderConstructor() {
      return builderConstructor.apply(this);
    }

    @Override
    final <R> R acceptRegular(DtoRegularGoalContext.RegularGoalContextCases<R> cases) {
      return cases.simple(this);
    }

    @Override
    public final <R> R acceptSimple(DtoSimpleGoal.SimpleGoalCases<R> cases) {
      return cases.regular(this);
    }

    final CodeBlock invocationParameters() {
      return CodeBlock.of(String.join(", ", parameterNames()));
    }
  }

  static final Function<SimpleRegularGoalContext, AbstractRegularGoalDetails> goalDetails =
      regularGoalContextCases(
          constructor -> constructor.details,
          method -> method.details);

  private static final Predicate<SimpleRegularGoalContext> isInstance =
      asPredicate(regularGoalContextCases(
          constructor -> false,
          method -> method.details.methodType == INSTANCE_METHOD));

  private static final Function<SimpleRegularGoalContext, List<? extends AbstractRegularStep>> regularSteps =
      regularGoalContextCases(
          constructor -> constructor.steps,
          method -> method.methodSteps());

  private static final Function<SimpleRegularGoalContext, Optional<FieldSpec>> fields =
      regularGoalContextCases(
          constructor -> empty(),
          method -> isInstance.test(method) ?
              Optional.of(method.field()) :
              empty());

  private static final Function<SimpleRegularGoalContext, MethodSpec> builderConstructor =
      regularGoalContextCases(
          constructor -> constructor(PRIVATE),
          method -> {
            if (!isInstance.test(method)
                || method.context.lifecycle == REUSE_INSTANCES) {
              return constructor(PRIVATE);
            }
            ClassName type = method.context.type;
            ParameterSpec parameter = parameterSpec(type, downcase(type.simpleName()));
            return constructorBuilder()
                .addParameter(parameter)
                .addStatement("this.$N = $N", method.field(), parameter)
                .addModifiers(PRIVATE)
                .build();
          });

  interface RegularGoalContextCases<R> {
    R constructorGoal(SimpleConstructorGoalContext goal);
    R methodGoal(SimpleMethodGoalContext goal);
  }

  static <R> Function<SimpleRegularGoalContext, R> asFunction(RegularGoalContextCases<R> cases) {
    return goal -> goal.acceptRegular(cases);
  }

  static <R> Function<SimpleRegularGoalContext, R> regularGoalContextCases(
      Function<SimpleConstructorGoalContext, R> constructor,
      Function<SimpleMethodGoalContext, R> method) {
    return asFunction(new RegularGoalContextCases<R>() {
      @Override
      public R constructorGoal(SimpleConstructorGoalContext goal) {
        return constructor.apply(goal);
      }
      @Override
      public R methodGoal(SimpleMethodGoalContext goal) {
        return method.apply(goal);
      }
    });
  }

  private DtoRegularGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
