package net.zerobuilder.compiler;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.GoalContext.GoalCases;
import net.zerobuilder.compiler.GoalContext.GoalFunction;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.GoalContext.always;
import static net.zerobuilder.compiler.ParameterContext.asStepInterface;

final class ContractContext {

  private static final GoalCases<ImmutableList<TypeSpec>> stepInterfaces = always(new GoalFunction<ImmutableList<TypeSpec>>() {
    @Override
    public ImmutableList<TypeSpec> apply(GoalContext goal, TypeName goalType, ImmutableList<? extends ParameterContext> parameters) {
      return FluentIterable.from(parameters).transform(asStepInterface).toList();
    }
  });

  static TypeSpec buildContract(GoalContext goal) {
    return classBuilder(goal.contractName)
        .addTypes(goal.accept(stepInterfaces))
        .addModifiers(PUBLIC, FINAL, STATIC)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

  private ContractContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
