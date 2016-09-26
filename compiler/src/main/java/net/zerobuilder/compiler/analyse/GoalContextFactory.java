package net.zerobuilder.compiler.analyse;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoBeanParameter.ValidBeanParameter;
import net.zerobuilder.compiler.analyse.DtoGoal.AbstractGoal;
import net.zerobuilder.compiler.analyse.DtoValidGoal.ValidBeanGoal;
import net.zerobuilder.compiler.analyse.DtoValidGoal.ValidRegularGoal;
import net.zerobuilder.compiler.analyse.DtoValidParameter.ValidParameter;
import net.zerobuilder.compiler.analyse.DtoValidParameter.ValidRegularParameter;
import net.zerobuilder.compiler.generate.DtoBeanGoalContext.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBuilders.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.ConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.MethodGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;
import net.zerobuilder.compiler.generate.DtoStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoStep.LoneGetterStep;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.upcase;
import static net.zerobuilder.compiler.analyse.DtoValidParameter.parameterName;

public final class GoalContextFactory {

  static AbstractGoalContext context(final DtoValidGoal.ValidGoal validGoal, final BuildersContext builders,
                                     final boolean toBuilder, final boolean builder) throws ValidationException {
    return validGoal.accept(new DtoValidGoal.ValidGoalCases<AbstractGoalContext>() {
      @Override
      public AbstractGoalContext regularGoal(ValidRegularGoal goal) {
        final ClassName contractName = contractName(goal.goal.goal, builders);
        final ImmutableList<TypeName> thrownTypes = thrownTypes(goal.goal.executableElement);
        final ImmutableList<RegularStep> steps = steps(contractName,
            goal.goal.goal.goalType,
            goal.parameters,
            thrownTypes,
            regularParameterFactory);
        return goal.goal.goal.accept(new DtoGoal.RegularGoalCases<AbstractGoalContext>() {
          @Override
          public AbstractGoalContext method(DtoGoal.MethodGoal goal) {
            return new MethodGoalContext(
                goal, builders, toBuilder, builder, contractName, steps, thrownTypes);
          }
          @Override
          public AbstractGoalContext constructor(DtoGoal.ConstructorGoal goal) {
            return new ConstructorGoalContext(
                goal, builders, toBuilder, builder, contractName, steps, thrownTypes);
          }
        });
      }
      @Override
      public AbstractGoalContext beanGoal(ValidBeanGoal goal) {
        ClassName contractName = contractName(goal.goal.goal, builders);
        ImmutableList<? extends AbstractBeanStep> steps = steps(contractName,
            goal.goal.goal.goalType,
            goal.parameters,
            ImmutableList.<TypeName>of(),
            beansParameterFactory);
        FieldSpec field = FieldSpec.builder(goal.goal.goal.goalType,
            downcase(goal.goal.goal.goalType.simpleName()), PRIVATE).build();
        return new BeanGoalContext(
            goal.goal.goal, builders, toBuilder, builder, contractName, steps, field);
      }
    });
  }

  private static <P extends ValidParameter, R extends AbstractStep>
  ImmutableList<R> steps(ClassName builderType,
                         TypeName nextType,
                         ImmutableList<P> parameters,
                         ImmutableList<TypeName> thrownTypes,
                         ParameterFactory<P, R> parameterFactory) {
    ImmutableList.Builder<R> builder = ImmutableList.builder();
    for (P parameter : parameters.reverse()) {
      ClassName thisType = builderType.nestedClass(upcase(parameter.acceptParameter(parameterName)));
      builder.add(parameterFactory.create(thisType, nextType, parameter, thrownTypes));
      nextType = thisType;
    }
    return builder.build().reverse();
  }

  private static abstract class ParameterFactory<P extends ValidParameter, R extends AbstractStep> {
    abstract R create(ClassName typeThisStep, TypeName typeNextStep, P parameter, ImmutableList<TypeName> declaredExceptions);
  }

  private static final ParameterFactory<ValidBeanParameter, ? extends AbstractBeanStep> beansParameterFactory
      = new ParameterFactory<ValidBeanParameter, AbstractBeanStep>() {
    @Override
    AbstractBeanStep create(final ClassName thisType, final TypeName nextType, final ValidBeanParameter validParameter, ImmutableList<TypeName> declaredExceptions) {
      return validParameter.accept(new DtoBeanParameter.BeanParameterCases<AbstractBeanStep>() {
        @Override
        public AbstractBeanStep accessorPair(DtoBeanParameter.AccessorPair pair) {
          String setter = "set" + upcase(pair.acceptParameter(parameterName));
          return new AccessorPairStep(thisType, nextType, pair, setter);
        }
        @Override
        public AbstractBeanStep loneGetter(DtoBeanParameter.LoneGetter loneGetter) {
          return new LoneGetterStep(thisType, nextType, loneGetter);
        }
      });
    }
  };

  private static final ParameterFactory<ValidRegularParameter, RegularStep> regularParameterFactory
      = new ParameterFactory<ValidRegularParameter, RegularStep>() {
    @Override
    RegularStep create(ClassName thisType, TypeName nextType, ValidRegularParameter validParameter, ImmutableList<TypeName> declaredExceptions) {
      FieldSpec field = FieldSpec.builder(validParameter.type, validParameter.name, PRIVATE).build();
      return new RegularStep(thisType, nextType, validParameter, declaredExceptions, field);
    }
  };

  private static ClassName contractName(AbstractGoal goal, BuildersContext config) {
    return config.generatedType.nestedClass(upcase(goal.name + "Builder"));
  }

  private static ImmutableList<TypeName> thrownTypes(ExecutableElement executableElement) {
    return FluentIterable
        .from(executableElement.getThrownTypes())
        .transform(new Function<TypeMirror, TypeName>() {
          @Override
          public TypeName apply(TypeMirror thrownType) {
            return TypeName.get(thrownType);
          }
        })
        .toList();
  }

  private GoalContextFactory() {
    throw new UnsupportedOperationException("no instances");
  }
}
