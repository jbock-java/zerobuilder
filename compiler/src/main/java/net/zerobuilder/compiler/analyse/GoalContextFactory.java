package net.zerobuilder.compiler.analyse;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.analyse.DtoPackage.GoalTypes.BeanGoalElement;
import net.zerobuilder.compiler.analyse.DtoPackage.GoalTypes.RegularGoalElement;
import net.zerobuilder.compiler.analyse.DtoShared.AbstractGoal;
import net.zerobuilder.compiler.analyse.DtoShared.ValidBeanParameter;
import net.zerobuilder.compiler.analyse.DtoShared.ValidGoal;
import net.zerobuilder.compiler.analyse.DtoShared.ValidGoal.ValidationResultCases;
import net.zerobuilder.compiler.analyse.DtoShared.ValidParameter;
import net.zerobuilder.compiler.analyse.DtoShared.ValidRegularParameter;
import net.zerobuilder.compiler.generate.BuildersType;
import net.zerobuilder.compiler.generate.GoalContext;
import net.zerobuilder.compiler.generate.GoalContext.AbstractContext;
import net.zerobuilder.compiler.generate.StepContext.AbstractStep;
import net.zerobuilder.compiler.generate.StepContext.BeansStep;
import net.zerobuilder.compiler.generate.StepContext.RegularStep;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

import static net.zerobuilder.compiler.Utilities.upcase;

public final class GoalContextFactory {

  static AbstractContext context(final ValidGoal validGoal, final BuildersType config,
                                             final boolean toBuilder, final boolean builder,
                                             final CodeBlock goalCall) throws ValidationException {
    return validGoal.accept(new ValidationResultCases<AbstractContext>() {
      @Override
      AbstractContext executableGoal(RegularGoalElement goal, ImmutableList<ValidRegularParameter> validParameters) {
        ClassName contractName = contractName(goal.goal, config);
        ImmutableList<TypeName> thrownTypes = thrownTypes(goal.executableElement);
        ImmutableList<RegularStep> parameters = steps(contractName,
            goal.goal.goalType,
            validParameters,
            thrownTypes,
            regularParameterFactory);
        return new GoalContext.RegularGoalContext(
            goal.goal,
            config,
            toBuilder,
            builder,
            contractName,
            thrownTypes,
            parameters,
            goalCall);
      }
      @Override
      AbstractContext beanGoal(BeanGoalElement goal, ImmutableList<ValidBeanParameter> validBeanParameters) {
        ClassName contractName = contractName(goal.goal, config);
        ImmutableList<BeansStep> parameters = steps(contractName,
            goal.goal.goalType,
            validBeanParameters,
            ImmutableList.<TypeName>of(),
            beansParameterFactory);
        return new GoalContext.BeanGoalContext(
            goal.goal,
            config,
            toBuilder,
            builder,
            contractName,
            parameters,
            goalCall);
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
      ClassName thisType = builderType.nestedClass(upcase(parameter.name));
      builder.add(parameterFactory.create(thisType, nextType, parameter, thrownTypes));
      nextType = thisType;
    }
    return builder.build().reverse();
  }

  private static abstract class ParameterFactory<P extends ValidParameter, R extends AbstractStep> {
    abstract R create(ClassName typeThisStep, TypeName typeNextStep, P parameter, ImmutableList<TypeName> declaredExceptions);
  }

  private static final ParameterFactory<ValidBeanParameter, BeansStep> beansParameterFactory
      = new ParameterFactory<ValidBeanParameter, BeansStep>() {
    @Override
    BeansStep create(ClassName typeThisStep, TypeName typeNextStep, ValidBeanParameter parameter, ImmutableList<TypeName> declaredExceptions) {
      return new BeansStep(typeThisStep, typeNextStep, parameter);
    }
  };

  private static final ParameterFactory<ValidRegularParameter, RegularStep> regularParameterFactory
      = new ParameterFactory<ValidRegularParameter, RegularStep>() {
    @Override
    RegularStep create(ClassName typeThisStep, TypeName typeNextStep, ValidRegularParameter parameter, ImmutableList<TypeName> declaredExceptions) {
      return new RegularStep(typeThisStep, typeNextStep, parameter, declaredExceptions);
    }
  };

  private static ClassName contractName(AbstractGoal goal, BuildersType config) {
    return config.generatedType.nestedClass(upcase(goal.name + "Builder"));
  }

  public enum GoalKind {
    CONSTRUCTOR, STATIC_METHOD, INSTANCE_METHOD
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
