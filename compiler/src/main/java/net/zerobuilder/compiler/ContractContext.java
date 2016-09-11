package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.GoalContext.GoalCases;

import javax.lang.model.element.Modifier;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.presentInstances;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.toArray;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.GoalContext.always;
import static net.zerobuilder.compiler.GoalContext.contractName;
import static net.zerobuilder.compiler.GoalContext.contractUpdaterName;

final class ContractContext {

  private static final GoalCases<ImmutableList<TypeSpec>> stepInterfaces
      = always(new Function<GoalContext, ImmutableList<TypeSpec>>() {
    @Override
    public ImmutableList<TypeSpec> apply(GoalContext goal) {
      ImmutableList.Builder<TypeSpec> specs = ImmutableList.builder();
      for (int i = 0; i < goal.goalParameters.size() - 1; i++) {
        ParameterContext spec = goal.goalParameters.get(i);
        specs.add(spec.asStepInterface(goal.maybeAddPublic()));
      }
      ParameterContext spec = getLast(goal.goalParameters);
      specs.add(spec.asStepInterface(goal.maybeAddPublic(), goal.thrownTypes));
      return specs.build();
    }
  });

  private static final GoalCases<Optional<TypeSpec>> updaterInterface
      = always(new Function<GoalContext, Optional<TypeSpec>>() {
    @Override
    public Optional<TypeSpec> apply(GoalContext goal) {
      if (!goal.toBuilder) {
        return absent();
      }
      MethodSpec buildMethod = methodBuilder("build")
          .returns(goal.goalType)
          .addModifiers(PUBLIC, ABSTRACT)
          .addExceptions(goal.thrownTypes)
          .build();
      return Optional.of(interfaceBuilder(goal.accept(contractUpdaterName))
          .addMethod(buildMethod)
          .addMethods(goal.accept(updateMethods))
          .addModifiers(toArray(goal.maybeAddPublic(), Modifier.class))
          .build());
    }
  });

  private static final GoalCases<ImmutableList<MethodSpec>> updateMethods
      = always(new Function<GoalContext, ImmutableList<MethodSpec>>() {
    @Override
    public ImmutableList<MethodSpec> apply(GoalContext goal) {
      ClassName updaterName = goal.accept(contractUpdaterName);
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (ParameterContext spec : goal.goalParameters) {
        builder.add(spec.asUpdaterInterfaceMethod(updaterName));
      }
      return builder.build();
    }
  });

  static TypeSpec buildContract(GoalContext goal) {
    return classBuilder(goal.accept(contractName))
        .addTypes(presentInstances(of(goal.accept(updaterInterface))))
        .addTypes(goal.accept(stepInterfaces))
        .addModifiers(toArray(goal.maybeAddPublic(FINAL, STATIC), Modifier.class))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

  private ContractContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
