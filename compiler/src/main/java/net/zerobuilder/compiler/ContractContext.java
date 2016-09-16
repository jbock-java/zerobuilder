package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.GoalContext.GoalCases;
import net.zerobuilder.compiler.GoalContext.GoalFunction;

import javax.lang.model.element.Modifier;

import static com.google.common.collect.Iterables.toArray;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.GoalContext.always;
import static net.zerobuilder.compiler.ParameterContext.asStepInterface;

final class ContractContext {

  private static final GoalCases<ImmutableList<TypeSpec>> stepInterfaces = always(new GoalFunction<ImmutableList<TypeSpec>>() {
    @Override
    public ImmutableList<TypeSpec> apply(GoalContext goal, TypeName goalType, ImmutableList<? extends ParameterContext> parameters) {
      ImmutableList.Builder<TypeSpec> builder = ImmutableList.builder();
      for (ParameterContext parameter : parameters) {
        builder.add(parameter.accept(asStepInterface));
      }
      return builder.build();
    }
  });

  static TypeSpec buildContract(GoalContext goal) {
    return classBuilder(goal.contractName)
        .addTypes(goal.accept(stepInterfaces))
        .addModifiers(toArray(goal.maybeAddPublic(FINAL, STATIC), Modifier.class))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

  private ContractContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
