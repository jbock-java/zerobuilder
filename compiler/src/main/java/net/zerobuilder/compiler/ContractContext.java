package net.zerobuilder.compiler;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.GoalContext.SharedGoalContext;

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

final class ContractContext {

  private final SharedGoalContext context;

  ContractContext(SharedGoalContext context) {
    this.context = context;
  }

  private ImmutableList<TypeSpec> stepInterfaces() {
    ImmutableList.Builder<TypeSpec> specs = ImmutableList.builder();
    for (int i = 0; i < context.parameters.size() - 1; i++) {
      ParameterContext spec = context.parameters.get(i);
      specs.add(spec.asStepInterface(context.maybeAddPublic()));
    }
    ParameterContext spec = getLast(context.parameters);
    specs.add(spec.asStepInterface(context.maybeAddPublic(), context.thrownTypes()));
    return specs.build();
  }

  private Optional<TypeSpec> updaterInterface() {
    if (!context.toBuilder) {
      return absent();
    }
    MethodSpec buildMethod = methodBuilder("build")
        .returns(context.goalType)
        .addModifiers(PUBLIC, ABSTRACT)
        .addExceptions(context.thrownTypes())
        .build();
    return Optional.of(interfaceBuilder(context.contractUpdaterName())
        .addMethod(buildMethod)
        .addMethods(updateMethods())
        .addModifiers(toArray(context.maybeAddPublic(), Modifier.class))
        .build());
  }

  private ImmutableList<MethodSpec> updateMethods() {
    ClassName updaterName = context.contractUpdaterName();
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    for (ParameterContext spec : context.parameters) {
      builder.add(spec.asUpdaterInterfaceMethod(updaterName));
    }
    return builder.build();
  }

  TypeSpec buildContract() {
    return classBuilder(context.contractName())
        .addTypes(presentInstances(of(updaterInterface())))
        .addTypes(stepInterfaces())
        .addModifiers(toArray(context.maybeAddPublic(FINAL, STATIC), Modifier.class))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

}
