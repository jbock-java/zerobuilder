package net.zerobuilder.compiler.analyse;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.analyse.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.analyse.DtoBeanParameter.LoneGetter;
import net.zerobuilder.compiler.analyse.DtoGoal.ConstructorGoalDetails;
import net.zerobuilder.compiler.analyse.DtoGoal.RegularGoalCases;
import net.zerobuilder.compiler.analyse.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.analyse.DtoParameter.RegularParameter;
import net.zerobuilder.compiler.analyse.DtoValidGoal.ValidBeanGoal;
import net.zerobuilder.compiler.analyse.DtoValidGoal.ValidGoal;
import net.zerobuilder.compiler.analyse.DtoValidGoal.ValidGoalCases;
import net.zerobuilder.compiler.analyse.DtoValidGoal.ValidRegularGoal;
import net.zerobuilder.compiler.generate.DtoBeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.LoneGetterStep;
import net.zerobuilder.compiler.generate.DtoGoalContext.IGoal;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

import static net.zerobuilder.compiler.Utilities.upcase;
import static net.zerobuilder.compiler.analyse.DtoParameter.parameterName;
import static net.zerobuilder.compiler.analyse.DtoValidGoal.goalName;
import static net.zerobuilder.compiler.analyse.DtoValidGoal.goalType;
import static net.zerobuilder.compiler.generate.DtoGoalContext.contractName;

public final class GoalContextFactory {

  static IGoal context(final ValidGoal validGoal, final ClassName generatedType) throws ValidationException {
    return validGoal.accept(new ValidGoalCases<IGoal>() {
      @Override
      public IGoal regularGoal(ValidRegularGoal goal) {
        final ImmutableList<TypeName> thrownTypes = thrownTypes(goal.goal.executableElement);
        final ImmutableList<RegularStep> steps = steps(goal,
            generatedType,
            goal.parameters,
            thrownTypes,
            regularParameterFactory);
        return goal.goal.goal.accept(new RegularGoalCases<IGoal>() {
          @Override
          public IGoal method(DtoGoal.MethodGoalDetails goal) {
            return new DtoRegularGoalContext.MethodGoal(goal, steps, thrownTypes);
          }
          @Override
          public IGoal constructor(ConstructorGoalDetails goal) {
            return new DtoRegularGoalContext.ConstructorGoal(goal, steps, thrownTypes);
          }
        });
      }
      @Override
      public IGoal beanGoal(ValidBeanGoal goal) {
        ImmutableList<? extends AbstractBeanStep> steps = steps(goal,
            generatedType,
            goal.parameters,
            ImmutableList.<TypeName>of(),
            beansParameterFactory);
        return DtoBeanGoalContext.BeanGoal.create(goal.goal.goal, steps);
      }
    });
  }

  private static <P extends AbstractParameter, S extends AbstractStep>
  ImmutableList<S> steps(ValidGoal goal,
                         ClassName generatedType,
                         ImmutableList<P> parameters,
                         ImmutableList<TypeName> thrownTypes,
                         ParameterFactory<P, S> parameterFactory) {
    ClassName contractName = contractName(goalName(goal), generatedType);
    TypeName nextType = goalType(goal);
    ImmutableList.Builder<S> builder = ImmutableList.builder();
    for (P parameter : parameters.reverse()) {
      ClassName thisType = contractName.nestedClass(upcase(parameterName.apply(parameter)));
      builder.add(parameterFactory.create(thisType, nextType, parameter, thrownTypes));
      nextType = thisType;
    }
    return builder.build().reverse();
  }

  private static abstract class ParameterFactory<P extends AbstractParameter, R extends AbstractStep> {
    abstract R create(ClassName typeThisStep, TypeName typeNextStep, P parameter, ImmutableList<TypeName> declaredExceptions);
  }

  private static final ParameterFactory<AbstractBeanParameter, ? extends AbstractBeanStep> beansParameterFactory
      = new ParameterFactory<AbstractBeanParameter, AbstractBeanStep>() {
    @Override
    AbstractBeanStep create(final ClassName thisType, final TypeName nextType, final AbstractBeanParameter validParameter, ImmutableList<TypeName> declaredExceptions) {
      return validParameter.accept(new DtoBeanParameter.BeanParameterCases<AbstractBeanStep>() {
        @Override
        public AbstractBeanStep accessorPair(AccessorPair pair) {
          String setter = "set" + upcase(parameterName.apply(pair));
          return AccessorPairStep.create(thisType, nextType, pair, setter);
        }
        @Override
        public AbstractBeanStep loneGetter(LoneGetter loneGetter) {
          return LoneGetterStep.create(thisType, nextType, loneGetter);
        }
      });
    }
  };

  private static final ParameterFactory<RegularParameter, RegularStep> regularParameterFactory
      = new ParameterFactory<RegularParameter, RegularStep>() {
    @Override
    RegularStep create(ClassName thisType, TypeName nextType, RegularParameter validParameter, ImmutableList<TypeName> declaredExceptions) {
      return RegularStep.create(thisType, nextType, validParameter, declaredExceptions);
    }
  };

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
}
