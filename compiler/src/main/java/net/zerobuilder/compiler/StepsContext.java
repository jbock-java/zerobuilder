package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.GoalContext.GoalCases;
import net.zerobuilder.compiler.GoalContextFactory.GoalKind;
import net.zerobuilder.compiler.ParameterContext.BeansParameterContext;
import net.zerobuilder.compiler.ParameterContext.RegularParameterContext;

import static com.google.common.collect.Iterables.getLast;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.GoalContext.builderImplName;
import static net.zerobuilder.compiler.GoalContext.stepInterfaceNames;
import static net.zerobuilder.compiler.GoalContextFactory.GoalKind.INSTANCE_METHOD;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.upcase;

final class StepsContext {

  private static final GoalCases<ImmutableList<FieldSpec>> fields
      = new GoalCases<ImmutableList<FieldSpec>>() {
    @Override
    ImmutableList<FieldSpec> regularGoal(GoalContext goal, TypeName goalType, GoalKind kind, ImmutableList<RegularParameterContext> parameters) {
      ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
      if (kind == INSTANCE_METHOD) {
        ClassName receiverType = goal.config.annotatedType;
        builder.add(FieldSpec.builder(receiverType, '_' + downcase(receiverType.simpleName()), PRIVATE).build());
      }
      for (RegularParameterContext parameter : parameters.subList(0, parameters.size() - 1)) {
        String name = parameter.parameter.name;
        builder.add(FieldSpec.builder(parameter.parameter.type, name, PRIVATE).build());
      }
      return builder.build();
    }
    @Override
    ImmutableList<FieldSpec> fieldGoal(GoalContext goal, ClassName goalType, ImmutableList<BeansParameterContext> parameters) {
      FieldSpec field = FieldSpec.builder(goalType, downcase(goalType.simpleName()))
          .build();
      return ImmutableList.of(field);
    }
  };

  private static final GoalCases<ImmutableList<MethodSpec>> stepsButLast
      = new GoalCases<ImmutableList<MethodSpec>>() {
    @Override
    ImmutableList<MethodSpec> regularGoal(GoalContext goal, TypeName goalType, GoalKind kind, ImmutableList<RegularParameterContext> parameters) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (RegularParameterContext parameter : parameters.subList(0, parameters.size() - 1)) {
        String name = parameter.parameter.name;
        TypeName type = parameter.parameter.type;
        builder.add(methodBuilder(name)
            .addAnnotation(Override.class)
            .returns(parameter.returnType)
            .addParameter(ParameterSpec.builder(type, name).build())
            .addStatement("this.$N = $N", name, name)
            .addStatement("return this")
            .addModifiers(PUBLIC)
            .build());
      }
      return builder.build();
    }
    @Override
    ImmutableList<MethodSpec> fieldGoal(GoalContext goal, ClassName goalType, ImmutableList<BeansParameterContext> parameters) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (BeansParameterContext parameter : parameters.subList(0, parameters.size() - 1)) {
        String name = parameter.parameter.name;
        TypeName type = parameter.parameter.type;
        if (parameter.parameter.setterlessCollection.isPresent()) {
          ClassName setterlessCollection = parameter.parameter.setterlessCollection.get();
          String iterationVarName = downcase(setterlessCollection.simpleName());
          builder.add(methodBuilder(name)
              .addAnnotation(Override.class)
              .returns(parameter.returnType)
              .addParameter(ParameterSpec.builder(type, name).build())
              .beginControlFlow("for ($T $N : $N)",
                  setterlessCollection, iterationVarName, name)
              .addStatement("this.$N.$N().add($N)",
                  downcase(goalType.simpleName()),
                  parameter.parameter.projectionMethodName,
                  iterationVarName)
              .endControlFlow()
              .addStatement("return this")
              .addModifiers(PUBLIC)
              .build());
        } else {
          builder.add(methodBuilder(name)
              .addAnnotation(Override.class)
              .returns(parameter.returnType)
              .addParameter(ParameterSpec.builder(type, name).build())
              .addStatement("this.$N.set$L($N)", downcase(goalType.simpleName()), upcase(name), name)
              .addStatement("return this")
              .addModifiers(PUBLIC)
              .build());
        }
      }
      return builder.build();
    }
  };

  private static final GoalCases<MethodSpec> lastStep = new GoalCases<MethodSpec>() {
    @Override
    MethodSpec regularGoal(GoalContext goal, TypeName goalType, GoalKind kind, ImmutableList<RegularParameterContext> parameters) {
      RegularParameterContext parameter = getLast(parameters);
      String name = parameter.parameter.name;
      TypeName type = parameter.parameter.type;
      return methodBuilder(parameter.parameter.name)
          .addAnnotation(Override.class)
          .addParameter(ParameterSpec.builder(type, name).build())
          .addExceptions(goal.thrownTypes)
          .addModifiers(PUBLIC)
          .returns(goalType)
          .addCode(goal.goalCall).build();
    }
    @Override
    MethodSpec fieldGoal(GoalContext goal, ClassName goalType, ImmutableList<BeansParameterContext> parameters) {
      BeansParameterContext parameter = getLast(parameters);
      String name = parameter.parameter.name;
      TypeName type = parameter.parameter.type;
      if (parameter.parameter.setterlessCollection.isPresent()) {
        ClassName setterlessCollection = parameter.parameter.setterlessCollection.get();
        String iterationVarName = downcase(setterlessCollection.simpleName());
        return methodBuilder(name)
            .addAnnotation(Override.class)
            .returns(parameter.returnType)
            .addParameter(ParameterSpec.builder(type, name).build())
            .beginControlFlow("for ($T $N : $N)",
                setterlessCollection, iterationVarName, name)
            .addStatement("this.$N.$N().add($N)",
                downcase(goalType.simpleName()),
                parameter.parameter.projectionMethodName,
                iterationVarName)
            .endControlFlow()
            .addCode(goal.goalCall)
            .addModifiers(PUBLIC)
            .build();
      } else {
        return methodBuilder(parameter.parameter.name)
            .addAnnotation(Override.class)
            .addParameter(ParameterSpec.builder(type, name).build())
            .addExceptions(goal.thrownTypes)
            .addModifiers(PUBLIC)
            .returns(goalType)
            .addStatement("this.$N.set$L($N)", downcase(goalType.simpleName()), upcase(name), name)
            .addCode(goal.goalCall).build();
      }
    }
  };

  static TypeSpec buildStepsImpl(GoalContext goal) {
    return classBuilder(goal.accept(builderImplName))
        .addSuperinterfaces(goal.accept(stepInterfaceNames))
        .addFields(goal.accept(fields))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .addMethods(goal.accept(stepsButLast))
        .addMethod(goal.accept(lastStep))
        .addModifiers(FINAL, STATIC)
        .build();
  }

  private StepsContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
