package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.GoalMethodType;
import net.zerobuilder.compiler.generate.DtoGoal.MethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.GoalCases;
import net.zerobuilder.compiler.generate.DtoGoalContext.IGoal;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static java.util.Optional.empty;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoGoal.GoalMethodType.INSTANCE_METHOD;
import static net.zerobuilder.compiler.generate.Utilities.asPredicate;
import static net.zerobuilder.compiler.generate.Utilities.constructor;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.fieldSpec;
import static net.zerobuilder.compiler.generate.Utilities.memoize;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;

final class DtoRegularGoal {

  interface RegularGoalContext extends AbstractGoalContext {

    <R> R acceptRegular(RegularGoalContextCases<R> cases);
    List<String> parameterNames();
    TypeName type();
  }

  interface RegularGoalContextCases<R> {
    R constructorGoal(ConstructorGoalContext goal);
    R methodGoal(MethodGoalContext goal);
  }

  static <R> Function<RegularGoalContext, R> asFunction(RegularGoalContextCases<R> cases) {
    return goal -> goal.acceptRegular(cases);
  }

  static <R> Function<RegularGoalContext, R> regularGoalContextCases(
      Function<ConstructorGoalContext, R> constructor,
      Function<MethodGoalContext, R> method) {
    return asFunction(new RegularGoalContextCases<R>() {
      @Override
      public R constructorGoal(ConstructorGoalContext goal) {
        return constructor.apply(goal);
      }
      @Override
      public R methodGoal(MethodGoalContext goal) {
        return method.apply(goal);
      }
    });
  }

  static final class ConstructorGoal implements IGoal {
    final ConstructorGoalDetails details;

    final List<RegularStep> steps;
    final List<TypeName> thrownTypes;

    private ConstructorGoal(ConstructorGoalDetails details,
                            List<RegularStep> steps,
                            List<TypeName> thrownTypes) {
      this.steps = steps;
      this.thrownTypes = thrownTypes;
      this.details = details;
    }

    static ConstructorGoal create(ConstructorGoalDetails details,
                                  List<RegularStep> steps,
                                  List<TypeName> thrownTypes) {
      return new ConstructorGoal(details, steps, thrownTypes);
    }

    @Override
    public AbstractGoalContext withContext(BuildersContext context) {
      return new ConstructorGoalContext(this, context);
    }
  }

  static final class ConstructorGoalContext
      implements RegularGoalContext {

    final ConstructorGoal goal;
    final BuildersContext context;

    ConstructorGoalContext(ConstructorGoal goal,
                           BuildersContext context) {
      this.goal = goal;
      this.context = context;
    }

    @Override
    public <R> R acceptRegular(RegularGoalContextCases<R> cases) {
      return cases.constructorGoal(this);
    }

    @Override
    public List<String> parameterNames() {
      return goal.details.parameterNames;
    }

    @Override
    public TypeName type() {
      return goal.details.goalType;
    }

    @Override
    public <R> R accept(GoalCases<R> cases) {
      return cases.regularGoal(this);
    }
  }

  static final class MethodGoal implements IGoal {
    final MethodGoalDetails details;

    final List<RegularStep> steps;
    final List<TypeName> thrownTypes;

    private MethodGoal(MethodGoalDetails details,
                       List<RegularStep> steps,
                       List<TypeName> thrownTypes) {
      this.steps = steps;
      this.thrownTypes = thrownTypes;
      this.details = details;
    }

    static MethodGoal create(MethodGoalDetails details,
                             List<RegularStep> steps,
                             List<TypeName> thrownTypes) {
      return new MethodGoal(details, steps, thrownTypes);
    }

    @Override
    public AbstractGoalContext withContext(BuildersContext context) {
      return new MethodGoalContext(this, context);
    }
  }

  static final class MethodGoalContext
      implements RegularGoalContext {

    final BuildersContext context;
    final MethodGoal goal;

    GoalMethodType methodType() {
      return goal.details.methodType;
    }

    private final Supplier<FieldSpec> field;

    /**
     * @return An instance of type {@link BuildersContext#type}.
     */
    FieldSpec field() {
      return field.get();
    }

    MethodGoalContext(MethodGoal goal,
                      BuildersContext context) {
      this.goal = goal;
      this.context = context;
      this.field = memoizeField(context);
    }

    private static Supplier<FieldSpec> memoizeField(BuildersContext context) {
      return memoize(() -> {
        ClassName type = context.type;
        String name = '_' + downcase(type.simpleName());
        return context.lifecycle == REUSE_INSTANCES
            ? fieldSpec(type, name, PRIVATE)
            : fieldSpec(type, name, PRIVATE, FINAL);
      });
    }

    @Override
    public <R> R acceptRegular(RegularGoalContextCases<R> cases) {
      return cases.methodGoal(this);
    }

    @Override
    public <R> R accept(GoalCases<R> cases) {
      return cases.regularGoal(this);
    }

    @Override
    public List<String> parameterNames() {
      return goal.details.parameterNames;
    }

    @Override
    public TypeName type() {
      return goal.details.goalType;
    }
  }

  static final Function<RegularGoalContext, RegularGoalDetails> goalDetails =
      regularGoalContextCases(
          cGoal -> cGoal.goal.details,
          mGoal -> mGoal.goal.details);

  static final Predicate<RegularGoalContext> isInstance =
      asPredicate(regularGoalContextCases(
          cGoal -> false,
          mGoal -> mGoal.goal.details.methodType == INSTANCE_METHOD));

  static final Function<RegularGoalContext, BuildersContext> buildersContext =
      regularGoalContextCases(
          cGoal -> cGoal.context,
          mGoal -> mGoal.context);

  static final Function<RegularGoalContext, List<RegularStep>> regularSteps =
      regularGoalContextCases(
          cGoal -> cGoal.goal.steps,
          mGoal -> mGoal.goal.steps);

  static final Function<RegularGoalContext, Optional<FieldSpec>> fields =
      regularGoalContextCases(
          cGoal -> empty(),
          mGoal -> isInstance.test(mGoal) ?
              Optional.of(mGoal.field()) :
              empty());

  static final Function<RegularGoalContext, MethodSpec> builderConstructor =
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

  private DtoRegularGoal() {
    throw new UnsupportedOperationException("no instances");
  }
}
