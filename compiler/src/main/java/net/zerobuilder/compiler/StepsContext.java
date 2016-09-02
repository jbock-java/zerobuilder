package net.zerobuilder.compiler;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Name;

import static com.google.common.collect.Iterables.getLast;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Util.downcase;

final class StepsContext {

  private final MyContext context;

  StepsContext(MyContext context) {
    this.context = context;
  }

  private ImmutableList<FieldSpec> fields() {
    ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
    Optional<ClassName> receiver = context.receiver();
    if (receiver.isPresent()) {
      ClassName r = receiver.get();
      builder.add(FieldSpec.builder(r, "_" + downcase(r.simpleName()), PRIVATE).build());
    }
    for (StepSpec stepSpec : context.stepSpecs.subList(0, context.stepSpecs.size() - 1)) {
      String name = stepSpec.parameter.getSimpleName().toString();
      builder.add(FieldSpec.builder(TypeName.get(stepSpec.parameter.asType()), name, PRIVATE).build());
    }
    return builder.build();
  }

  private MethodSpec constructor() {
    return constructorBuilder()
        .addModifiers(PRIVATE)
        .build();
  }

  private ImmutableList<MethodSpec> stepsButLast() {
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    for (StepSpec stepSpec : context.stepSpecs.subList(0, context.stepSpecs.size() - 1)) {
      ParameterSpec parameter = stepSpec.parameter();
      builder.add(methodBuilder(parameter.name)
          .addAnnotation(Override.class)
          .returns(stepSpec.returnType)
          .addParameter(parameter)
          .addStatement("this.$N = $N", parameter.name, parameter.name)
          .addStatement("return this")
          .addModifiers(PUBLIC)
          .build());
    }
    return builder.build();
  }

  private MethodSpec lastStep() {
    StepSpec stepSpec = getLast(context.stepSpecs);
    MethodSpec.Builder builder = methodBuilder(stepSpec.parameter.getSimpleName().toString())
        .addAnnotation(Override.class)
        .addParameter(stepSpec.parameter())
        .addExceptions(context.thrownTypes())
        .addModifiers(PUBLIC)
        .returns(context.goalType);
    Name buildVia = context.goal.getSimpleName();
    String returnLiteral = TypeName.VOID.equals(context.goalType) ? "" : "return ";
    Optional<ClassName> receiver = context.receiver();
    return (context.goal.getKind() == CONSTRUCTOR
        ? builder.addStatement("return new $T($L)", context.buildElement, context.factoryCallArgs())
        : receiver.isPresent()
        ? builder.addStatement("$L $N.$N($L)", returnLiteral, "_" + downcase(receiver.get().simpleName()), buildVia, context.factoryCallArgs())
        : builder.addStatement("$L $T.$N($L)", returnLiteral, context.buildElement, buildVia, context.factoryCallArgs()))
        .build();
  }

  TypeSpec buildStepsImpl() {
    return classBuilder(context.stepsImplTypeName())
        .addSuperinterfaces(context.stepInterfaceNames())
        .addFields(fields())
        .addMethod(constructor())
        .addMethods(stepsButLast())
        .addMethod(lastStep())
        .addModifiers(FINAL, STATIC)
        .build();
  }


}
