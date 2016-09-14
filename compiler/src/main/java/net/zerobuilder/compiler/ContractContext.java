package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.GoalContext.GoalCases;
import net.zerobuilder.compiler.GoalContext.GoalFunction;

import javax.lang.model.element.Modifier;

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.toArray;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.GoalContext.always;
import static net.zerobuilder.compiler.GoalContext.contractName;

final class ContractContext {

  private static final GoalCases<ImmutableList<TypeSpec>> stepInterfaces = always(new GoalFunction<ImmutableList<TypeSpec>>() {
    @Override
    public ImmutableList<TypeSpec> apply(GoalContext goal, TypeName goalType) {
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

  static TypeSpec buildContract(GoalContext goal) {
    return classBuilder(goal.accept(contractName))
        .addTypes(goal.accept(stepInterfaces))
        .addModifiers(toArray(goal.maybeAddPublic(FINAL, STATIC), Modifier.class))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

  private ContractContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
